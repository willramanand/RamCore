package dev.willram.ramcore.stat;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Supplies {@link StatModifier}s for a player. Sources are collected by a {@link StatService} when
 * it builds a {@link StatSnapshot}.
 *
 * <p>A source is called on whatever thread asked for the snapshot. Sources that read live entity
 * state (equipment, held item) must therefore be consulted on the player's own thread — the
 * service's invalidation listeners already run there.</p>
 */
@FunctionalInterface
public interface StatSource {

    /**
     * The modifiers this source contributes for the player, or an empty collection when it has
     * none. Never returns {@code null}.
     *
     * @param player the player
     * @return the modifiers
     */
    @NotNull
    Collection<StatModifier> modifiers(@NotNull Player player);
}
