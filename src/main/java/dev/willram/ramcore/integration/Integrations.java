package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

/**
 * Entry points for RamCore optional integration helpers.
 */
public final class Integrations {

    @NotNull
    public static IntegrationRegistry registry() {
        return IntegrationRegistry.create();
    }

    @NotNull
    public static IntegrationRegistry standard() {
        return IntegrationRegistry.standard();
    }

    @NotNull
    public static IntegrationRegistry standard(@NotNull PluginDetector detector) {
        return IntegrationRegistry.standard(detector);
    }

    private Integrations() {
    }
}
