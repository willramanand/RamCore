package dev.willram.ramcore.worldinstance;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * The {@code ramcore-instance.json} marker written into every instanced-world directory. Its presence
 * lets the startup sweep identify and delete leftover instance directories from a previous run.
 *
 * @param template  the template the instance was copied from
 * @param id        the short instance id
 * @param createdAt epoch millis when created
 */
public record InstanceMarker(@NotNull String template, @NotNull String id, long createdAt) {

    /** The marker file name inside an instance directory. */
    public static final String FILE_NAME = "ramcore-instance.json";

    private static final Pattern FIELD = Pattern.compile("\"(template|id|createdAt)\"\\s*:\\s*(\"[^\"]*\"|\\d+)");

    public InstanceMarker {
        requireNonNull(template, "template");
        requireNonNull(id, "id");
    }

    /** Whether a directory carries an instance marker. */
    public static boolean present(@NotNull Path directory) {
        return Files.isRegularFile(directory.resolve(FILE_NAME));
    }

    /** Writes this marker into a directory. */
    public void writeTo(@NotNull Path directory) throws IOException {
        Files.createDirectories(directory);
        String json = "{\n"
                + "  \"template\": \"" + escape(this.template) + "\",\n"
                + "  \"id\": \"" + escape(this.id) + "\",\n"
                + "  \"createdAt\": " + this.createdAt + "\n"
                + "}\n";
        Files.writeString(directory.resolve(FILE_NAME), json, StandardCharsets.UTF_8);
    }

    /** Reads the marker from a directory, if present and parseable. */
    @NotNull
    public static Optional<InstanceMarker> readFrom(@NotNull Path directory) {
        Path file = directory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String template = "";
            String id = "";
            long createdAt = 0L;
            Matcher matcher = FIELD.matcher(content);
            while (matcher.find()) {
                String key = matcher.group(1);
                String raw = matcher.group(2);
                String value = raw.startsWith("\"") ? raw.substring(1, raw.length() - 1) : raw;
                switch (key) {
                    case "template" -> template = value;
                    case "id" -> id = value;
                    case "createdAt" -> createdAt = Long.parseLong(value);
                    default -> {
                    }
                }
            }
            return Optional.of(new InstanceMarker(template, id, createdAt));
        } catch (IOException | RuntimeException failure) {
            return Optional.empty();
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
