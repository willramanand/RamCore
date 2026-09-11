package dev.willram.ramcore.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Sends logical packet visual operations.
 */
public interface PacketVisualTransport {

    void send(@NotNull PacketViewer viewer, @NotNull PacketVisualOperation operation);
}
