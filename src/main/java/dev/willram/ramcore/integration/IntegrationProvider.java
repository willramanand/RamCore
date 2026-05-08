package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

/**
 * Capability provider for one optional integration.
 */
public interface IntegrationProvider {

    @NotNull
    IntegrationDescriptor descriptor();

    @NotNull
    IntegrationSnapshot snapshot();

    default boolean available() {
        return snapshot().available();
    }

    default boolean supports(@NotNull IntegrationCapability capability) {
        return descriptor().supports(capability);
    }
}
