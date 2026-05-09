package dev.willram.ramcore.packet;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Packet utilities facade.
 */
public final class Packets {

    @NotNull
    public static PacketVisualSession session(@NotNull PacketViewer viewer, @NotNull PacketVisualTransport transport) {
        return new PacketVisualSession(viewer, transport);
    }

    @NotNull
    public static InMemoryPacketVisualTransport memoryTransport() {
        return new InMemoryPacketVisualTransport();
    }

    @NotNull
    public static PacketDiagnostics diagnostics(boolean protocolLibAvailable) {
        return PacketDiagnostics.of(protocolLibAvailable);
    }

    @NotNull
    public static NmsAccessRegistry registerProtocolCapability(@NotNull NmsAccessRegistry registry, boolean protocolLibAvailable) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.PACKETS,
                protocolLibAvailable ? NmsSupportStatus.PARTIAL : NmsSupportStatus.UNSUPPORTED,
                NmsAccessTier.RAMCORE_ADAPTER,
                "ramcore-packet-visuals",
                null,
                null,
                protocolLibAvailable
                        ? "ProtocolLib is available for viewer-scoped packet visuals; concrete packet factories must remain version guarded."
                        : "ProtocolLib is not available; only in-memory visual state planning is usable."
        ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.PACKET_VISUAL_STATE,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-packet-visuals",
                        "RamCore can model viewer-scoped packet visual state without mutating server state."
                ))
                .override(new NmsCapabilityCheck(
                        NmsCapability.PACKET_PROTOCOLLIB_TRANSPORT,
                        protocolLibAvailable ? NmsSupportStatus.PARTIAL : NmsSupportStatus.UNSUPPORTED,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-packet-visuals",
                        null,
                        null,
                        protocolLibAvailable
                                ? "ProtocolLib transport is available; concrete packets still need version-aware factories."
                                : "ProtocolLib is missing, so packet sends are unavailable."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.PACKET_VERSIONED_FACTORIES,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Concrete metadata, entity spawn, equipment, and scoreboard packets need guarded versioned factories."
                ));
        return registry;
    }

    private Packets() {
    }
}
