package dev.willram.ramcore.exception;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base for exceptions that report every validation problem at once rather than failing on the first.
 *
 * <p>Subtypes ({@code ConfigValidationException}, {@code TemplateValidationException},
 * {@code ContentValidationException}) keep their own constructors and {@link #errors()} contract;
 * {@link #validationErrors()} exposes the structured form with source and path.</p>
 */
public abstract class ValidationException extends RuntimeException {
    private final List<ValidationError> validationErrors;

    protected ValidationException(@NotNull String message, @NotNull List<ValidationError> validationErrors) {
        super(message);
        this.validationErrors = List.copyOf(validationErrors);
    }

    /**
     * The structured errors, with source and path where known.
     *
     * @return the validation errors
     */
    @NotNull
    public List<ValidationError> validationErrors() {
        return this.validationErrors;
    }

    /**
     * The errors as single-line strings.
     *
     * @return the descriptions
     */
    @NotNull
    public List<String> errors() {
        return this.validationErrors.stream().map(ValidationError::describe).toList();
    }

    /**
     * Wraps plain messages as sourceless {@link ValidationError}s.
     *
     * @param messages the messages
     * @return the errors
     */
    @NotNull
    protected static List<ValidationError> fromMessages(@NotNull List<String> messages) {
        return messages.stream().map(ValidationError::of).toList();
    }
}
