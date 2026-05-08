package dev.willram.ramcore.template;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reports all template reference validation errors.
 */
public final class TemplateValidationException extends RuntimeException {
    private final List<String> errors;

    public TemplateValidationException(@NotNull List<String> errors) {
        super("template validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    @NotNull
    public List<String> errors() {
        return this.errors;
    }
}
