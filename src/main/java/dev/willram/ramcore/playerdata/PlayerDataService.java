package dev.willram.ramcore.playerdata;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.service.Service;
import dev.willram.ramcore.service.ServiceKey;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Loads per-player values before a player joins, hands them out synchronously while the player is
 * online, and writes them back on quit, on a timer, and at shutdown.
 *
 * <p>Each value is identified by a {@link PlayerDataKey} and persisted through any
 * {@link Store}{@code <UUID, T>} from {@code dev.willram.ramcore.store}. The lifecycle is:</p>
 *
 * <ol>
 *   <li>{@code AsyncPlayerPreLoginEvent} (login thread): {@link #preload(UUID)} fans a load out
 *   across every registered key.</li>
 *   <li>{@code PlayerLoginEvent} disallowed: {@link #cancelPending(UUID)} drops the load.</li>
 *   <li>{@code PlayerJoinEvent}: {@link #join(Player)} promotes the loaded values or applies the
 *   {@link JoinPolicy}.</li>
 *   <li>{@code PlayerQuitEvent}: {@link #quit(UUID)} saves dirty keys and evicts.</li>
 * </ol>
 *
 * <p>Thread contract (the Folia rule for this subsystem): values are read and written on the
 * player's own thread after {@link #join(Player)}. Mutations are announced with
 * {@link #markDirty(UUID, PlayerDataKey)} or replaced with {@link #set(UUID, PlayerDataKey, Object)}.
 * Before an async save the value is copied on the player's scheduler through
 * {@link PlayerDataKey#snapshot()}, then written on the async scheduler, so {@code T} need not be
 * thread-safe. {@link #quit(UUID)} and {@link #saveAll()} copy inline because they already run on
 * the owning thread (quit event) or at shutdown.</p>
 *
 * <p>Install from {@link RamPlugin#load()} with {@link #install(RamPlugin, PlayerDataOptions)} and
 * register keys before players join; keys registered later are loaded for players already online.
 * Consumers reach the service through {@code services().get(PlayerDataService.KEY)}.</p>
 *
 * <p>Stability: experimental. Folia-safe by design.</p>
 */
public interface PlayerDataService extends Service, Terminable {

    ServiceKey<PlayerDataService> KEY = ServiceKey.of("player-data", PlayerDataService.class);

    /**
     * A service with no plugin attached: nothing listens to Bukkit events, so the caller drives
     * {@link #preload}, {@link #join}, {@link #quit} directly. Timers start on {@link #enable}.
     *
     * @param options tuning
     * @return the service
     */
    @NotNull
    static PlayerDataService create(@NotNull PlayerDataOptions options) {
        return new SimplePlayerDataService(options, Clock.systemUTC(), null);
    }

    /**
     * A service with no plugin attached and an injectable clock for the pending sweep.
     *
     * @param options tuning
     * @param clock   time source
     * @return the service
     */
    @NotNull
    static PlayerDataService create(@NotNull PlayerDataOptions options, @NotNull Clock clock) {
        return new SimplePlayerDataService(options, clock, null);
    }

    /**
     * Registers the service under {@link #KEY}, registers the Bukkit listener on enable and binds
     * the service so it flushes on disable. Call from {@link RamPlugin#load()}: the service
     * registry refuses registrations after load.
     *
     * @param plugin  the owning plugin
     * @param options tuning
     * @return the service, for key registration
     */
    @NotNull
    static PlayerDataService install(@NotNull RamPlugin plugin, @NotNull PlayerDataOptions options) {
        SimplePlayerDataService service = new SimplePlayerDataService(options, Clock.systemUTC(), plugin);
        plugin.services().register(KEY, service);
        return service;
    }

    @NotNull
    PlayerDataOptions options();

    /**
     * Registers a key with its persistence. Players already online (or mid-login) get this key
     * loaded immediately.
     *
     * @param key   the key
     * @param store persistence keyed by player id; typically {@code Stores.cached(..)}
     * @param <T>   value type
     * @throws dev.willram.ramcore.exception.ApiMisuseException when the key is already registered
     */
    <T> void register(@NotNull PlayerDataKey<T> key, @NotNull Store<UUID, T> store);

    @NotNull
    Set<PlayerDataKey<?>> keys();

    // ---- reads and writes (player thread) ----

    /**
     * The value for an online, loaded player.
     *
     * @param playerId the player
     * @param key      the key
     * @param <T>      value type
     * @return the value, or empty when the player is not loaded or the key is unknown
     */
    @NotNull
    <T> Optional<T> get(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key);

    @NotNull
    default <T> Optional<T> get(@NotNull Player player, @NotNull PlayerDataKey<T> key) {
        return get(player.getUniqueId(), key);
    }

    /**
     * The value for an online, loaded player.
     *
     * @param playerId the player
     * @param key      the key
     * @param <T>      value type
     * @return the value
     * @throws dev.willram.ramcore.exception.ApiMisuseException when not loaded
     */
    @NotNull
    <T> T require(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key);

    @NotNull
    default <T> T require(@NotNull Player player, @NotNull PlayerDataKey<T> key) {
        return require(player.getUniqueId(), key);
    }

    /**
     * Replaces a value and marks it dirty.
     *
     * @param playerId the player
     * @param key      the key
     * @param value    the new value
     * @param <T>      value type
     * @throws dev.willram.ramcore.exception.ApiMisuseException when not loaded
     */
    <T> void set(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key, @NotNull T value);

    default <T> void set(@NotNull Player player, @NotNull PlayerDataKey<T> key, @NotNull T value) {
        set(player.getUniqueId(), key, value);
    }

    /**
     * Marks a value as modified in place so the next save writes it.
     *
     * @param playerId the player
     * @param key      the key
     */
    void markDirty(@NotNull UUID playerId, @NotNull PlayerDataKey<?> key);

    default void markDirty(@NotNull Player player, @NotNull PlayerDataKey<?> key) {
        markDirty(player.getUniqueId(), key);
    }

    boolean isDirty(@NotNull UUID playerId, @NotNull PlayerDataKey<?> key);

    /**
     * Whether every key has loaded and the player has joined.
     *
     * @param playerId the player
     * @return true when reads will succeed
     */
    boolean isLoaded(@NotNull UUID playerId);

    /**
     * Whether a load is in flight or waiting for the join.
     *
     * @param playerId the player
     * @return true when preloaded but not yet loaded
     */
    boolean isPending(@NotNull UUID playerId);

    /**
     * Completes once the player is loaded; already complete for a loaded player. Fails when the
     * load failed, the login was cancelled, or the player is unknown. Continuations that touch the
     * player must pick the player's {@code TaskContext}.
     *
     * @param playerId the player
     * @return readiness
     */
    @NotNull
    Promise<Void> whenReady(@NotNull UUID playerId);

    @NotNull
    default Promise<Void> whenReady(@NotNull Player player) {
        return whenReady(player.getUniqueId());
    }

    @NotNull
    Set<UUID> loadedPlayers();

    // ---- persistence ----

    /**
     * Saves this player's dirty keys. Values are copied on the player's scheduler first when the
     * player is online.
     *
     * @param playerId the player
     * @return completes when every write is durable
     */
    @NotNull
    Promise<Void> save(@NotNull UUID playerId);

    /**
     * Saves every dirty key of every loaded player, copying on each player's scheduler.
     *
     * @return number of key writes issued
     */
    @NotNull
    Promise<Integer> saveDirty();

    /**
     * Saves every key of every loaded player, copying inline. Meant for shutdown, where the server
     * is single-threaded; {@link #disable} calls it with a bounded wait.
     *
     * @return number of key writes issued
     */
    @NotNull
    Promise<Integer> saveAll();

    // ---- lifecycle hooks (driven by PlayerDataListener) ----

    /**
     * Starts loading every key for a player about to log in. Idempotent while the load is pending.
     *
     * @param playerId the player
     * @return completes when every key has loaded
     */
    @NotNull
    Promise<Void> preload(@NotNull UUID playerId);

    /**
     * Drops a pending load whose login was cancelled. Loaded players are not affected.
     *
     * @param playerId the player
     * @return true when a pending entry was removed
     */
    boolean cancelPending(@NotNull UUID playerId);

    /**
     * Promotes a preloaded player, or applies the {@link JoinPolicy} when the load is still
     * running. A player with no preload (installed mid-session) is loaded now.
     *
     * @param player the joining player
     */
    void join(@NotNull Player player);

    /**
     * Saves dirty keys inline and evicts the player.
     *
     * @param playerId the player
     * @return completes when the quit save is durable
     */
    @NotNull
    Promise<Void> quit(@NotNull UUID playerId);

    /**
     * Evicts pending entries older than four load timeouts that never joined.
     *
     * @return number of entries evicted
     */
    int sweepPending();

    /**
     * Flushes every loaded player (bounded by {@link PlayerDataOptions#flushTimeout()}), stops
     * timers and unregisters the listener. Idempotent; {@link #disable} calls it.
     */
    @Override
    void close();
}
