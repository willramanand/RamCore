package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for plugin/module diagnostic providers.
 */
public final class DiagnosticRegistry {
    private final Map<String, DiagnosticProvider> providers = new LinkedHashMap<>();

    @NotNull
    public static DiagnosticRegistry create() {
        return new DiagnosticRegistry();
    }

    @NotNull
    public DiagnosticRegistry register(@NotNull DiagnosticProvider provider) {
        Objects.requireNonNull(provider, "provider");
        RamPreconditions.checkArgument(!this.providers.containsKey(provider.id()), "diagnostic provider already registered",
                "Use a unique diagnostic provider id.");
        this.providers.put(provider.id(), provider);
        return this;
    }

    @NotNull
    public Optional<DiagnosticProvider> provider(@NotNull String id) {
        return Optional.ofNullable(this.providers.get(Objects.requireNonNull(id, "id")));
    }

    @NotNull
    public List<DiagnosticProvider> providers() {
        return this.providers.values().stream()
                .sorted(Comparator.comparing(DiagnosticProvider::category).thenComparing(DiagnosticProvider::id))
                .toList();
    }

    @NotNull
    public List<String> lines() {
        return providers().stream()
                .flatMap(provider -> provider.lines().stream()
                        .map(line -> provider.category() + "." + provider.id() + ": " + line))
                .toList();
    }
}
