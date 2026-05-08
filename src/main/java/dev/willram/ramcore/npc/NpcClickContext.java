package dev.willram.ramcore.npc;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Context passed to NPC click handlers.
 */
public record NpcClickContext(
        @NotNull NpcHandle<?> npc,
        @NotNull Player player,
        @NotNull Entity entity,
        @NotNull NpcClickType type
) {
}
