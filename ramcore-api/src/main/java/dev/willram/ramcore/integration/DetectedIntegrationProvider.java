package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Integration provider backed by plugin presence detection.
 */
public class DetectedIntegrationProvider implements IntegrationProvider {
    private final IntegrationDescriptor descriptor;
    private final PluginDetector detector;

    public DetectedIntegrationProvider(@NotNull IntegrationDescriptor descriptor, @NotNull PluginDetector detector) {
        this.descriptor = requireNonNull(descriptor, "descriptor");
        this.detector = requireNonNull(detector, "detector");
    }

    @NotNull
    @Override
    public IntegrationDescriptor descriptor() {
        return this.descriptor;
    }

    @NotNull
    @Override
    public IntegrationSnapshot snapshot() {
        String pluginName = this.descriptor.pluginName();
        try {
            if (!this.detector.present(pluginName)) {
                return new IntegrationSnapshot(this.descriptor, IntegrationStatus.MISSING, null, "plugin is not installed");
            }
            String version = this.detector.version(pluginName).orElse(null);
            if (!this.detector.enabled(pluginName)) {
                return new IntegrationSnapshot(this.descriptor, IntegrationStatus.DISABLED, version, "plugin is installed but disabled");
            }
            return new IntegrationSnapshot(this.descriptor, IntegrationStatus.AVAILABLE, version, "plugin is available");
        } catch (RuntimeException exception) {
            return new IntegrationSnapshot(this.descriptor, IntegrationStatus.ERROR, null, exception.getMessage());
        }
    }
}
