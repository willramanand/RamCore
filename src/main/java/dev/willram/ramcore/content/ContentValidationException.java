package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ValidationError;
import dev.willram.ramcore.exception.ValidationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown by {@link ContentLoadResult#throwIfErrors()} when a content load produced any errors.
 */
public final class ContentValidationException extends ValidationException {

    public ContentValidationException(@NotNull List<ValidationError> errors) {
        super("content validation failed: " + String.join("; ", errors.stream().map(ValidationError::describe).toList()), errors);
    }
}
