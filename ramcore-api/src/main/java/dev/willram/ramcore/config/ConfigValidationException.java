package dev.willram.ramcore.config;

import dev.willram.ramcore.exception.ValidationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown when loaded config values fail key requirements or validators. Now a
 * {@link ValidationException}; the {@code (List<String>)} constructor and {@link #errors()} are
 * unchanged.
 */
public final class ConfigValidationException extends ValidationException {

    public ConfigValidationException(@NotNull List<String> errors) {
        super("configuration validation failed: " + String.join("; ", errors), fromMessages(errors));
    }
}
