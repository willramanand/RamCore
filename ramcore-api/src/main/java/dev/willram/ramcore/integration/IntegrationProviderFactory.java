package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * SPI for supplying a specialised {@link IntegrationProvider} for a descriptor. Discovered via
 * {@link java.util.ServiceLoader} by {@link IntegrationRegistry#standard(PluginDetector)}, so a
 * module (for example {@code ramcore-protocol}) can contribute a provider that does a deeper check
 * than plain plugin detection without the api module depending on it.
 */
public interface IntegrationProviderFactory {

    /**
     * Supplies a provider for the descriptor, or empty to fall back to plain detection.
     *
     * @param descriptor the integration descriptor
     * @param detector   the plugin detector
     * @return a provider, or empty
     */
    @NotNull
    Optional<IntegrationProvider> create(@NotNull IntegrationDescriptor descriptor, @NotNull PluginDetector detector);
}
