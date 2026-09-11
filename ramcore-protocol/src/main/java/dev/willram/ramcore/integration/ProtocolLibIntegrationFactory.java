package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Contributes the ProtocolLib-aware {@link IntegrationProvider}. Registered via
 * {@code META-INF/services} so {@link IntegrationRegistry#standard(PluginDetector)} finds it when the
 * protocol module is on the classpath, without the api module depending on ProtocolLib.
 */
public final class ProtocolLibIntegrationFactory implements IntegrationProviderFactory {

    @NotNull
    @Override
    public Optional<IntegrationProvider> create(@NotNull IntegrationDescriptor descriptor, @NotNull PluginDetector detector) {
        if (descriptor.id().equals(StandardIntegrations.PROTOCOL_LIB.id())) {
            return Optional.of(new ProtocolLibIntegrationProvider(detector));
        }
        return Optional.empty();
    }
}
