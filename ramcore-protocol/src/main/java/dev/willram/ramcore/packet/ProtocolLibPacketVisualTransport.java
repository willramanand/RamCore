package dev.willram.ramcore.packet;

import com.comphenix.protocol.events.PacketContainer;
import dev.willram.ramcore.protocol.Protocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * ProtocolLib-backed transport for packet visual sessions.
 */
public final class ProtocolLibPacketVisualTransport implements PacketVisualTransport {
    private final Function<UUID, Optional<Player>> playerResolver;
    private final ProtocolVisualPacketFactory packetFactory;

    public ProtocolLibPacketVisualTransport(@NotNull Function<UUID, Optional<Player>> playerResolver,
                                            @NotNull ProtocolVisualPacketFactory packetFactory) {
        this.playerResolver = Objects.requireNonNull(playerResolver, "playerResolver");
        this.packetFactory = Objects.requireNonNull(packetFactory, "packetFactory");
    }

    @NotNull
    public static ProtocolLibPacketVisualTransport onlinePlayers(@NotNull ProtocolVisualPacketFactory packetFactory) {
        return new ProtocolLibPacketVisualTransport(id -> Optional.ofNullable(Bukkit.getPlayer(id)), packetFactory);
    }

    @Override
    public void send(@NotNull PacketViewer viewer, @NotNull PacketVisualOperation operation) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(operation, "operation");
        Optional<Player> player = this.playerResolver.apply(viewer.id());
        if (player.isEmpty()) {
            return;
        }
        List<PacketContainer> packets = this.packetFactory.createPackets(operation);
        for (PacketContainer packet : packets) {
            Protocol.sendPacketScheduled(player.get(), packet);
        }
    }
}
