package dev.willram.ramcore.integration;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Registry for optional integration providers.
 */
public final class IntegrationRegistry {
    private final Map<IntegrationId, IntegrationProvider> providers = new LinkedHashMap<>();

    @NotNull
    public static IntegrationRegistry create() {
        return new IntegrationRegistry();
    }

    @NotNull
    public static IntegrationRegistry standard() {
        return standard(new BukkitPluginDetector());
    }

    @NotNull
    public static IntegrationRegistry standard(@NotNull PluginDetector detector) {
        requireNonNull(detector, "detector");
        IntegrationRegistry registry = create();
        for (IntegrationDescriptor descriptor : StandardIntegrations.all()) {
            if (descriptor.id().equals(StandardIntegrations.PROTOCOL_LIB.id())) {
                registry.register(new ProtocolLibIntegrationProvider(detector));
            } else {
                registry.register(new DetectedIntegrationProvider(descriptor, detector));
            }
        }
        return registry;
    }

    @NotNull
    public IntegrationRegistry register(@NotNull IntegrationProvider provider) {
        requireNonNull(provider, "provider");
        IntegrationId id = provider.descriptor().id();
        RamPreconditions.checkArgument(!this.providers.containsKey(id), "integration already registered", "Use a unique integration id.");
        this.providers.put(id, provider);
        return this;
    }

    @NotNull
    public Optional<IntegrationProvider> provider(@NotNull IntegrationId id) {
        return Optional.ofNullable(this.providers.get(requireNonNull(id, "id")));
    }

    @NotNull
    public IntegrationProvider require(@NotNull IntegrationId id) {
        return provider(id).orElseThrow(() -> new IllegalArgumentException("Unknown integration: " + id));
    }

    public boolean available(@NotNull IntegrationId id) {
        return provider(id).map(IntegrationProvider::available).orElse(false);
    }

    public boolean supports(@NotNull IntegrationCapability capability) {
        requireNonNull(capability, "capability");
        return this.providers.values().stream()
                .anyMatch(provider -> provider.available() && provider.supports(capability));
    }

    @NotNull
    public List<IntegrationSnapshot> snapshots() {
        return this.providers.values().stream()
                .map(IntegrationProvider::snapshot)
                .sorted(Comparator.comparing(snapshot -> snapshot.descriptor().id()))
                .toList();
    }

    @NotNull
    public List<IntegrationProvider> providers() {
        return List.copyOf(this.providers.values());
    }
}
