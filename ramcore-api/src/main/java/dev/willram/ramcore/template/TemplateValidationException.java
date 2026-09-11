package dev.willram.ramcore.template;

import dev.willram.ramcore.exception.ValidationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reports all template reference validation errors. Now a {@link ValidationException}; the
 * {@code (List<String>)} constructor and {@link #errors()} are unchanged.
 */
public final class TemplateValidationException extends ValidationException {

    public TemplateValidationException(@NotNull List<String> errors) {
        super("template validation failed: " + String.join("; ", errors), fromMessages(errors));
    }
}
