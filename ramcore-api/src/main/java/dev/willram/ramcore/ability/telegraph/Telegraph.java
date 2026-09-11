package dev.willram.ramcore.ability.telegraph;

import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A server-side aim preview: something a caster (and nearby players) can see that shows where an
 * ability will land &mdash; a particle ring, a beam line, a ground marker, or a glow on the locked-on
 * targets. Everything is rendered in the world; there is no client HUD.
 *
 * <p>{@link #show(Player)} begins rendering a live preview around the caster's current aim and keeps
 * it updated until the returned {@link Terminable} is closed. Built-ins in {@link Telegraphs} redraw
 * on a repeating task on the caster's (or the marker location's) scheduler, so they are Folia-safe;
 * closing the returned terminable stops the task and clears any persistent state (glow, display
 * markers).</p>
 */
@FunctionalInterface
public interface Telegraph {

    /**
     * Begins showing the preview for {@code caster}.
     *
     * @param caster the casting player
     * @return a terminable that clears the preview when closed
     */
    @NotNull
    Terminable show(@NotNull Player caster);
}
