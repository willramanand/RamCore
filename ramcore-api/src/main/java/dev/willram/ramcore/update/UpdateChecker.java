package dev.willram.ramcore.update;

import dev.willram.ramcore.gson.GsonProvider;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Checks GitHub Releases for a newer RamCore version, once, on the async scheduler. It only reads
 * the latest release tag, compares it with {@link SemVer}, and logs one line; it never downloads
 * anything.
 *
 * <p>Stability: stable. The parsing helpers are pure and unit-tested; the network call is opt-in.</p>
 */
public final class UpdateChecker {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private UpdateChecker() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Extracts the {@code tag_name} from a GitHub "latest release" JSON body.
     *
     * @param json the response body
     * @return the tag, or empty when absent
     */
    @NotNull
    public static Optional<String> extractTag(@NotNull String json) {
        requireNonNull(json, "json");
        try {
            var object = GsonProvider.readObject(json);
            if (object.has("tag_name") && !object.get("tag_name").isJsonNull()) {
                return Optional.of(object.get("tag_name").getAsString());
            }
        } catch (RuntimeException ignored) {
            // malformed body -> no tag
        }
        return Optional.empty();
    }

    /**
     * Compares a running version with a latest tag.
     *
     * @param currentVersion the running version
     * @param latestTag      the latest release tag
     * @return the result, or empty when either fails to parse
     */
    @NotNull
    public static Optional<UpdateCheckResult> evaluate(@NotNull String currentVersion, @NotNull String latestTag) {
        requireNonNull(currentVersion, "currentVersion");
        requireNonNull(latestTag, "latestTag");
        try {
            SemVer current = SemVer.parse(currentVersion);
            SemVer latest = SemVer.parse(latestTag);
            return Optional.of(new UpdateCheckResult(current, latest, latest.compareTo(current) > 0));
        } catch (IllegalArgumentException notVersions) {
            return Optional.empty();
        }
    }

    /**
     * Checks {@code github.com/<repo>/releases/latest} once on the async scheduler and logs the
     * result. {@code repo} is {@code owner/name}.
     *
     * @param currentVersion the running version (e.g. {@code plugin.getPluginMeta().getVersion()})
     * @param repo           the GitHub repository, {@code owner/name}
     * @return the result, or empty when the check could not be completed
     */
    @NotNull
    public static Promise<Optional<UpdateCheckResult>> check(@NotNull String currentVersion, @NotNull String repo) {
        requireNonNull(currentVersion, "currentVersion");
        requireNonNull(repo, "repo");
        return Schedulers.async().supply(() -> fetch(repo).flatMap(tag -> evaluate(currentVersion, tag)))
                .thenApplyAsync(result -> {
                    result.ifPresentOrElse(
                            checkResult -> RamLog.info(checkResult.describe()),
                            () -> RamLog.info("RamCore update check did not complete."));
                    return result;
                });
    }

    private static Optional<String> fetch(@NotNull String repo) {
        String url = "https://api.github.com/repos/" + repo + "/releases/latest";
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return Optional.empty();
            }
            return extractTag(response.body());
        } catch (Exception e) {
            RamLog.info("RamCore update check failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
