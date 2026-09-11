package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Static metadata for an optional integration.
 */
public record IntegrationDescriptor(
        @NotNull IntegrationId id,
        @NotNull String pluginName,
        @NotNull Set<IntegrationCapability> capabilities,
        @NotNull String description
) {

    public IntegrationDescriptor {
        requireNonNull(id, "id");
        requireNonNull(pluginName, "pluginName");
        requireNonNull(capabilities, "capabilities");
        requireNonNull(description, "description");
        capabilities = Set.copyOf(capabilities);
    }

    public boolean supports(@NotNull IntegrationCapability capability) {
        return this.capabilities.contains(requireNonNull(capability, "capability"));
    }
}
