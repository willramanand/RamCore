package dev.willram.ramcore.region;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.serialize.Position;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Tracks which {@link RuleRegion}s each player currently stands in and fires
 * {@link RegionEnterEvent}/{@link RegionExitEvent} on transitions.
 *
 * <p>Register it as a listener (it watches move, teleport, world change, join and quit) and bind it
 * for cleanup. The transition logic ({@link #transition}) is separate from Bukkit wiring so it can
 * be tested directly with positions. Folia: the move event runs on the player's region; the current
 * sets live in a concurrent map.</p>
 *
 * <p>Stability: experimental. Only block-position changes are considered, so intra-block movement is
 * ignored.</p>
 */
public final class RegionTracker implements Listener, Terminable {

    /**
     * Receives region transitions. The default handler fires Bukkit events for online players.
     */
    @FunctionalInterface
    public interface TransitionHandler {
        void handle(@NotNull UUID playerId, @Nullable Player player, @NotNull RuleRegion region, boolean entered);
    }

    private final RegionRuleEngine engine;
    private final TransitionHandler handler;
    private final Map<UUID, Set<ContentId>> current = new ConcurrentHashMap<>();
    private boolean closed;

    private RegionTracker(@NotNull RegionRuleEngine engine, @NotNull TransitionHandler handler) {
        this.engine = requireNonNull(engine, "engine");
        this.handler = requireNonNull(handler, "handler");
    }

    /**
     * A tracker that fires {@link RegionEnterEvent}/{@link RegionExitEvent} for online players.
     *
     * @param engine the rule engine
     * @return the tracker
     */
    @NotNull
    public static RegionTracker create(@NotNull RegionRuleEngine engine) {
        return new RegionTracker(engine, RegionTracker::fireBukkitEvent);
    }

    /**
     * A tracker with a custom transition handler (for tests or custom dispatch).
     *
     * @param engine  the rule engine
     * @param handler the handler
     * @return the tracker
     */
    @NotNull
    public static RegionTracker create(@NotNull RegionRuleEngine engine, @NotNull TransitionHandler handler) {
        return new RegionTracker(engine, handler);
    }

    /**
     * The regions the player currently stands in.
     *
     * @param playerId the player id
     * @return an immutable snapshot of the current region ids
     */
    @NotNull
    public Set<ContentId> current(@NotNull UUID playerId) {
        return Set.copyOf(this.current.getOrDefault(requireNonNull(playerId, "playerId"), Set.of()));
    }

    /**
     * Recomputes the player's regions at a position and fires transitions for the difference.
     *
     * @param playerId the player id
     * @param player   the player, when available (passed to the handler); may be null in tests
     * @param position the new position
     */
    public void transition(@NotNull UUID playerId, @Nullable Player player, @NotNull Position position) {
        requireNonNull(playerId, "playerId");
        requireNonNull(position, "position");

        Map<ContentId, RuleRegion> now = new java.util.LinkedHashMap<>();
        for (RuleRegion region : this.engine.regionsAt(position)) {
            now.put(region.id(), region);
        }
        Set<ContentId> previous = this.current.getOrDefault(playerId, Set.of());

        for (ContentId id : previous) {
            if (!now.containsKey(id)) {
                this.engine.region(id).ifPresent(region -> this.handler.handle(playerId, player, region, false));
            }
        }
        for (RuleRegion region : now.values()) {
            if (!previous.contains(region.id())) {
                this.handler.handle(playerId, player, region, true);
            }
        }

        if (now.isEmpty()) {
            this.current.remove(playerId);
        } else {
            this.current.put(playerId, new LinkedHashSet<>(now.keySet()));
        }
    }

    /**
     * Clears the player's tracked regions, firing exit transitions for each.
     *
     * @param playerId the player id
     * @param player   the player, when available
     */
    public void clear(@NotNull UUID playerId, @Nullable Player player) {
        Set<ContentId> previous = this.current.remove(requireNonNull(playerId, "playerId"));
        if (previous == null) {
            return;
        }
        for (ContentId id : previous) {
            this.engine.region(id).ifPresent(region -> this.handler.handle(playerId, player, region, false));
        }
    }

    // ---- Bukkit wiring ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (event.getTo() == null || !event.hasChangedBlock()) {
            return;
        }
        transition(event.getPlayer().getUniqueId(), event.getPlayer(), Position.of(event.getTo()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }
        transition(event.getPlayer().getUniqueId(), event.getPlayer(), Position.of(event.getTo()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(@NotNull PlayerChangedWorldEvent event) {
        transition(event.getPlayer().getUniqueId(), event.getPlayer(), Position.of(event.getPlayer().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        transition(event.getPlayer().getUniqueId(), event.getPlayer(), Position.of(event.getPlayer().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId(), event.getPlayer());
    }

    private static void fireBukkitEvent(@NotNull UUID playerId, @Nullable Player player, @NotNull RuleRegion region, boolean entered) {
        if (player == null) {
            return;
        }
        Bukkit.getPluginManager().callEvent(entered ? new RegionEnterEvent(player, region) : new RegionExitEvent(player, region));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        HandlerList.unregisterAll(this);
        this.current.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
