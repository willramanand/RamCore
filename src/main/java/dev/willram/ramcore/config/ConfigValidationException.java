package dev.willram.ramcore.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown when loaded config values fail key requirements or validators.
 */
public final class ConfigValidationException extends ConfigException {
    private final List<String> errors;

    public ConfigValidationException(@NotNull List<String> errors) {
        super("configuration validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    @NotNull
    public List<String> errors() {
        return this.errors;
    }
}
