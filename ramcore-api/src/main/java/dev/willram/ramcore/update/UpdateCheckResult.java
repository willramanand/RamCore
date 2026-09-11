package dev.willram.ramcore.update;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * The outcome of an update check.
 *
 * @param current         the running version
 * @param latest          the latest published version
 * @param updateAvailable whether {@code latest} is newer than {@code current}
 */
public record UpdateCheckResult(@NotNull SemVer current, @NotNull SemVer latest, boolean updateAvailable) {

    public UpdateCheckResult {
        requireNonNull(current, "current");
        requireNonNull(latest, "latest");
    }

    @NotNull
    public String describe() {
        return this.updateAvailable
                ? "An update is available: " + this.latest + " (running " + this.current + ")"
                : "RamCore is up to date (" + this.current + ")";
    }
}
