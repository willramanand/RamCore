package dev.willram.ramcore.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * One aggregated validation error: an optional source (a file or resource), an optional path (a key
 * or id within it), and a human-readable message.
 *
 * @param source where the error is, such as a file name; null when not applicable
 * @param path   the path within the source, such as a config path or content id; null when none
 * @param message what is wrong
 */
public record ValidationError(@Nullable String source, @Nullable String path, @NotNull String message) {

    public ValidationError {
        requireNonNull(message, "message");
    }

    @NotNull
    public static ValidationError of(@NotNull String message) {
        return new ValidationError(null, null, message);
    }

    @NotNull
    public static ValidationError at(@Nullable String source, @Nullable String path, @NotNull String message) {
        return new ValidationError(source, path, message);
    }

    /**
     * A single-line rendering: {@code [source] path: message}, omitting the parts that are null.
     *
     * @return the description
     */
    @NotNull
    public String describe() {
        StringBuilder builder = new StringBuilder();
        if (this.source != null) {
            builder.append('[').append(this.source).append("] ");
        }
        if (this.path != null && !this.path.isEmpty()) {
            builder.append(this.path).append(": ");
        }
        builder.append(this.message);
        return builder.toString();
    }
}
