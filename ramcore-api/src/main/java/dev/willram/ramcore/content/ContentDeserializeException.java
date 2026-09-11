package dev.willram.ramcore.content;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown by a {@link ContentDeserializer} when a node cannot be turned into a spec. The
 * {@link SpecLoader} catches it and records a {@link dev.willram.ramcore.exception.ValidationError}
 * with the definition's source.
 */
public final class ContentDeserializeException extends RuntimeException {

    public ContentDeserializeException(@NotNull String message) {
        super(message);
    }

    public ContentDeserializeException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
