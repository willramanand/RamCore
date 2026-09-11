package dev.willram.ramcore.packet;

import com.comphenix.protocol.events.PacketContainer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Converts logical visual operations into ProtocolLib packets.
 */
@FunctionalInterface
public interface ProtocolVisualPacketFactory {

    @NotNull
    List<PacketContainer> createPackets(@NotNull PacketVisualOperation operation);
}
