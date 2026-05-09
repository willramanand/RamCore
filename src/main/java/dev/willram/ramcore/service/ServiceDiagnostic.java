package dev.willram.ramcore.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Runtime view of one registered service for diagnostics.
 */
public record ServiceDiagnostic(
        @NotNull String id,
        @NotNull String type,
        @NotNull List<String> dependencies,
        @NotNull String state
) {

    public ServiceDiagnostic {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        Objects.requireNonNull(state, "state");
    }
}
