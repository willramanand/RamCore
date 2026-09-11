package dev.willram.ramcore.integration;

import com.comphenix.protocol.ProtocolManager;
import dev.willram.ramcore.protocol.Protocol;
import org.jetbrains.annotations.NotNull;

/**
 * ProtocolLib adapter for packet-level capability checks.
 */
public final class ProtocolLibIntegrationProvider extends DetectedIntegrationProvider {

    public ProtocolLibIntegrationProvider(@NotNull PluginDetector detector) {
        super(StandardIntegrations.PROTOCOL_LIB, detector);
    }

    @NotNull
    public ProtocolManager manager() {
        IntegrationSnapshot snapshot = snapshot();
        if (!snapshot.available()) {
            throw new IllegalStateException("ProtocolLib is not available: " + snapshot.message());
        }
        return Protocol.manager();
    }
}
