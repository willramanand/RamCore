package dev.willram.ramcore.stat;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.service.Service;
import dev.willram.ramcore.service.ServiceKey;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Collects {@link StatSource}s and hands out cached, immutable {@link StatSnapshot}s per player.
 *
 * <p>A snapshot is computed on first request and cached until the player's stats change. The bundled
 * listener invalidates on {@code PlayerItemHeldEvent} and {@code PlayerArmorChangeEvent} and evicts
 * on {@code PlayerQuitEvent}; buff and party changes invalidate through their sources'
 * {@code onChange} callback ({@code service::invalidate}). Because a snapshot is read on the thread
 * that requested it and sources read live entity state, request snapshots on the player's thread.</p>
 *
 * <p>Install from {@link RamPlugin#load()} with {@link #install(RamPlugin, StatRegistry)} and add
 * sources before players join; consumers reach the service through
 * {@code services().get(StatService.KEY)}. Stability: experimental.</p>
 */
public interface StatService extends Service, Terminable {

    ServiceKey<StatService> KEY = ServiceKey.of("stats", StatService.class);

    /**
     * A service with no plugin attached: nothing listens to Bukkit events, so the caller drives
     * {@link #invalidate(UUID)} directly. Useful in tests.
     *
     * @param registry the stat definitions
     * @return the service
     */
    @NotNull
    static StatService create(@NotNull StatRegistry registry) {
        return new SimpleStatService(registry, null);
    }

    /**
     * Registers the service under {@link #KEY}, registers the invalidation listener on enable, and
     * binds the service so it clears on disable. Call from {@link RamPlugin#load()}.
     *
     * @param plugin   the owning plugin
     * @param registry the stat definitions
     * @return the service, for source registration
     */
    @NotNull
    static StatService install(@NotNull RamPlugin plugin, @NotNull StatRegistry registry) {
        SimpleStatService service = new SimpleStatService(registry, plugin);
        plugin.services().register(KEY, service);
        return service;
    }

    /** The stat definitions this service resolves against. */
    @NotNull
    StatRegistry registry();

    /**
     * Adds a source. Sources added later affect snapshots computed after they are added; call
     * {@link #invalidate(UUID)} to force a rebuild for an online player.
     *
     * @param source the source
     */
    void addSource(@NotNull StatSource source);

    /**
     * The player's snapshot, computing and caching it if needed.
     *
     * @param player the player
     * @return the snapshot
     */
    @NotNull
    StatSnapshot snapshot(@NotNull Player player);

    /**
     * Drops the cached snapshot for a player so the next {@link #snapshot(Player)} rebuilds it.
     *
     * @param playerId the player
     */
    void invalidate(@NotNull UUID playerId);

    /**
     * Drops the cached snapshot for a player.
     *
     * @param player the player
     */
    default void invalidate(@NotNull Player player) {
        invalidate(player.getUniqueId());
    }
}
