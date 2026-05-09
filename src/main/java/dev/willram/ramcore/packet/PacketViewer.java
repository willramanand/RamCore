package dev.willram.ramcore.packet;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable viewer identity for packet-only visual state.
 */
public record PacketViewer(@NotNull UUID id, @NotNull String name) {

    public PacketViewer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    @NotNull
    public static PacketViewer of(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return new PacketViewer(player.getUniqueId(), player.getName());
    }
}
