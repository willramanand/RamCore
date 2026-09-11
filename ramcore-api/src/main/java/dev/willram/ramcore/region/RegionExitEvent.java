package dev.willram.ramcore.region;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Fired by {@link RegionTracker} when a player leaves a {@link RuleRegion}. Not cancellable.
 */
public final class RegionExitEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final RuleRegion region;

    public RegionExitEvent(@NotNull Player player, @NotNull RuleRegion region) {
        this.player = requireNonNull(player, "player");
        this.region = requireNonNull(region, "region");
    }

    @NotNull
    public Player getPlayer() {
        return this.player;
    }

    @NotNull
    public RuleRegion getRegion() {
        return this.region;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
