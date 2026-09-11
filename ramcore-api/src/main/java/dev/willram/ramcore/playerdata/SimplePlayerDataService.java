package dev.willram.ramcore.playerdata;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.scheduler.Ticks;
import dev.willram.ramcore.service.ServiceContext;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.utils.RamLog;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

/**
 * Default {@link PlayerDataService}. One {@link Entry} per player moves through
 * pending (preloaded, not joined) to loaded (joined and every key present) to evicted.
 */
final class SimplePlayerDataService implements PlayerDataService {
    private final PlayerDataOptions options;
    private final Clock clock;
    private final @Nullable RamPlugin plugin;
    private final PlayerDataListener listener;
    private final Map<PlayerDataKey<?>, Registration<?>> registrations = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final List<Task> timers = new ArrayList<>();
    private volatile boolean enabled;
    private volatile boolean closed;

    SimplePlayerDataService(@NotNull PlayerDataOptions options, @NotNull Clock clock, @Nullable RamPlugin plugin) {
        this.options = requireNonNull(options, "options");
        this.clock = requireNonNull(clock, "clock");
        this.plugin = plugin;
        this.listener = new PlayerDataListener(this);
    }

    @NotNull
    @Override
    public PlayerDataOptions options() {
        return this.options;
    }

    // ---- registration ----

    @Override
    public <T> void register(@NotNull PlayerDataKey<T> key, @NotNull Store<UUID, T> store) {
        requireNonNull(key, "key");
        requireNonNull(store, "store");
        RamPreconditions.checkState(!this.closed, "player data service is closed", "Register keys from load() or enable(), not after disable.");
        Registration<T> registration = new Registration<>(key, store);
        synchronized (this.registrations) {
            RamPreconditions.checkState(!this.registrations.containsKey(key), "player data key already registered: " + key, "Register each key once, from one plugin.");
            this.registrations.put(key, registration);
        }
        // late registration: players already in the lifecycle get this key loaded now
        for (Entry entry : this.entries.values()) {
            entry.addLoad(loadKey(entry, registration));
        }
    }

    @NotNull
    @Override
    public Set<PlayerDataKey<?>> keys() {
        synchronized (this.registrations) {
            return Set.copyOf(this.registrations.keySet());
        }
    }

    @NotNull
    private List<Registration<?>> registrationsSnapshot() {
        synchronized (this.registrations) {
            return List.copyOf(this.registrations.values());
        }
    }

    // ---- reads and writes ----

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key) {
        requireNonNull(playerId, "playerId");
        requireNonNull(key, "key");
        Entry entry = this.entries.get(playerId);
        if (entry == null || !entry.loaded) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) entry.values.get(key));
    }

    @NotNull
    @Override
    public <T> T require(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key) {
        return get(playerId, key).orElseThrow(() -> RamPreconditions.misuse(
                "player data " + key + " is not loaded for " + playerId,
                "Read after join under JoinPolicy.KICK, or wait on whenReady(player) under JoinPolicy.DEFER."));
    }

    @Override
    public <T> void set(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key, @NotNull T value) {
        requireNonNull(playerId, "playerId");
        requireNonNull(key, "key");
        requireNonNull(value, "value");
        Entry entry = loadedEntry(playerId, key);
        entry.values.put(key, value);
        entry.dirty.add(key);
    }

    @Override
    public void markDirty(@NotNull UUID playerId, @NotNull PlayerDataKey<?> key) {
        requireNonNull(playerId, "playerId");
        requireNonNull(key, "key");
        loadedEntry(playerId, key).dirty.add(key);
    }

    @Override
    public boolean isDirty(@NotNull UUID playerId, @NotNull PlayerDataKey<?> key) {
        Entry entry = this.entries.get(requireNonNull(playerId, "playerId"));
        return entry != null && entry.dirty.contains(requireNonNull(key, "key"));
    }

    @NotNull
    private Entry loadedEntry(@NotNull UUID playerId, @NotNull PlayerDataKey<?> key) {
        Entry entry = this.entries.get(playerId);
        RamPreconditions.checkState(entry != null && entry.loaded && entry.values.containsKey(key),
                "player data " + key + " is not loaded for " + playerId,
                "Write only while the player is online and loaded; check isLoaded(playerId) first.");
        return entry;
    }

    @Override
    public boolean isLoaded(@NotNull UUID playerId) {
        Entry entry = this.entries.get(requireNonNull(playerId, "playerId"));
        return entry != null && entry.loaded;
    }

    @Override
    public boolean isPending(@NotNull UUID playerId) {
        Entry entry = this.entries.get(requireNonNull(playerId, "playerId"));
        return entry != null && !entry.loaded;
    }

    @NotNull
    @Override
    public Promise<Void> whenReady(@NotNull UUID playerId) {
        Entry entry = this.entries.get(requireNonNull(playerId, "playerId"));
        if (entry == null) {
            return Promise.exceptionally(new IllegalStateException("no player data lifecycle for " + playerId + " (not preloaded, or already quit)"));
        }
        return Promise.wrapFuture(entry.ready);
    }

    @NotNull
    @Override
    public Set<UUID> loadedPlayers() {
        Set<UUID> loaded = new LinkedHashSet<>();
        for (Entry entry : this.entries.values()) {
            if (entry.loaded) {
                loaded.add(entry.id);
            }
        }
        return Set.copyOf(loaded);
    }

    // ---- persistence ----

    @NotNull
    @Override
    public Promise<Void> save(@NotNull UUID playerId) {
        Entry entry = this.entries.get(requireNonNull(playerId, "playerId"));
        if (entry == null || !entry.loaded) {
            return Promise.completed(null);
        }
        return Promise.wrapFuture(saveEntry(entry, entry.takeDirty(), entry.player != null));
    }

    @NotNull
    @Override
    public Promise<Integer> saveDirty() {
        List<CompletableFuture<Void>> writes = new ArrayList<>();
        int count = 0;
        for (Entry entry : this.entries.values()) {
            if (!entry.loaded) {
                continue;
            }
            Set<PlayerDataKey<?>> dirty = entry.takeDirty();
            if (dirty.isEmpty()) {
                continue;
            }
            count += dirty.size();
            writes.add(saveEntry(entry, dirty, entry.player != null));
        }
        return countWhenDone(writes, count);
    }

    @NotNull
    @Override
    public Promise<Integer> saveAll() {
        List<PendingWrite> writes = saveEverythingInline();
        return countWhenDone(writes.stream().map(PendingWrite::future).toList(), writes.size());
    }

    @NotNull
    private List<PendingWrite> saveEverythingInline() {
        List<PendingWrite> writes = new ArrayList<>();
        for (Entry entry : this.entries.values()) {
            if (!entry.loaded) {
                continue;
            }
            entry.dirty.clear();
            for (PlayerDataKey<?> key : entry.values.keySet()) {
                writes.add(new PendingWrite(entry.id, key, writeInline(entry, key)));
            }
        }
        return writes;
    }

    @NotNull
    private static Promise<Integer> countWhenDone(@NotNull List<CompletableFuture<Void>> writes, int count) {
        if (writes.isEmpty()) {
            return Promise.completed(0);
        }
        return Promise.wrapFuture(CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenApply(ignored -> count));
    }

    /**
     * Writes the given keys. With {@code hop} the values are copied on the player's scheduler
     * first; without it they are copied on the calling thread, which must then own the player.
     */
    @NotNull
    private CompletableFuture<Void> saveEntry(@NotNull Entry entry, @NotNull Set<PlayerDataKey<?>> keys, boolean hop) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        if (!hop) {
            return CompletableFuture.allOf(keys.stream().map(key -> writeInline(entry, key)).toArray(CompletableFuture[]::new));
        }
        Player player = requireNonNull(entry.player, "player");
        return Schedulers.call(TaskContext.of(player), () -> snapshotAll(entry, keys))
                .toCompletableFuture()
                .thenCompose(snapshots -> {
                    List<CompletableFuture<Void>> writes = new ArrayList<>(snapshots.size());
                    snapshots.forEach((key, value) -> writes.add(writeSnapshot(entry.id, key, value)));
                    return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
                })
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        // the player's scheduler refused (entity retired mid-save); quit saves separately
                        entry.dirty.addAll(keys);
                    }
                });
    }

    @NotNull
    private Map<PlayerDataKey<?>, Object> snapshotAll(@NotNull Entry entry, @NotNull Set<PlayerDataKey<?>> keys) {
        Map<PlayerDataKey<?>, Object> snapshots = new LinkedHashMap<>();
        for (PlayerDataKey<?> key : keys) {
            Object value = entry.values.get(key);
            if (value != null) {
                snapshots.put(key, snapshot(key, value));
            }
        }
        return snapshots;
    }

    @NotNull
    private CompletableFuture<Void> writeInline(@NotNull Entry entry, @NotNull PlayerDataKey<?> key) {
        Object value = entry.values.get(key);
        if (value == null) {
            return CompletableFuture.completedFuture(null);
        }
        return writeSnapshot(entry.id, key, snapshot(key, value));
    }

    @SuppressWarnings("unchecked")
    private static <T> T snapshot(@NotNull PlayerDataKey<T> key, @NotNull Object value) {
        return key.copy((T) value);
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<Void> writeSnapshot(@NotNull UUID playerId, @NotNull PlayerDataKey<T> key, @NotNull Object value) {
        Registration<T> registration = (Registration<T>) this.registrations.get(key);
        if (registration == null) {
            return CompletableFuture.completedFuture(null);
        }
        return registration.store.save(playerId, (T) value).toCompletableFuture().whenComplete((ignored, error) -> {
            if (error != null) {
                RamLog.warn("failed to save player data " + key + " for " + playerId, error);
            }
        });
    }

    // ---- lifecycle ----

    @NotNull
    @Override
    public Promise<Void> preload(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        RamPreconditions.checkState(!this.closed, "player data service is closed", "Do not preload after disable.");
        Entry entry = this.entries.computeIfAbsent(playerId, id -> {
            Entry created = new Entry(id, this.clock.instant());
            for (Registration<?> registration : registrationsSnapshot()) {
                created.addLoad(loadKey(created, registration));
            }
            return created;
        });
        return Promise.wrapFuture(entry.loadFuture());
    }

    @NotNull
    private <T> CompletableFuture<Void> loadKey(@NotNull Entry entry, @NotNull Registration<T> registration) {
        PlayerDataKey<T> key = registration.key;
        return registration.store.load(entry.id).toCompletableFuture().thenAccept(loaded -> {
            if (loaded.isPresent()) {
                entry.values.put(key, loaded.get());
            } else {
                entry.values.put(key, key.newDefault());
                entry.dirty.add(key);
            }
        });
    }

    @Override
    public boolean cancelPending(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        Entry entry = this.entries.get(playerId);
        if (entry == null || entry.loaded) {
            return false;
        }
        this.entries.remove(playerId, entry);
        entry.ready.completeExceptionally(new IllegalStateException("login cancelled for " + playerId));
        return true;
    }

    @Override
    public void join(@NotNull Player player) {
        requireNonNull(player, "player");
        UUID playerId = player.getUniqueId();
        if (!this.entries.containsKey(playerId)) {
            preload(playerId);
        }
        Entry entry = this.entries.get(playerId);
        if (entry == null) {
            return;
        }
        entry.player = player;
        entry.joined = true;
        CompletableFuture<Void> load = entry.loadFuture();
        if (load.isDone()) {
            promote(entry, load);
            return;
        }
        load.whenComplete((ignored, error) -> promote(entry, load));
        if (this.options.joinPolicy() == JoinPolicy.KICK) {
            long timeoutTicks = Math.max(1L, Ticks.from(this.options.loadTimeout().toMillis(), TimeUnit.MILLISECONDS));
            Schedulers.runLater(TaskContext.of(player), timeoutTicks, () -> {
                if (!entry.loaded && this.entries.get(playerId) == entry) {
                    RamLog.warn("player data for " + player.getName() + " did not load within " + this.options.loadTimeout().toMillis() + "ms; kicking");
                    kick(entry);
                }
            });
        }
    }

    private void promote(@NotNull Entry entry, @NotNull CompletableFuture<Void> load) {
        if (this.entries.get(entry.id) != entry) {
            return; // cancelled or quit while loading
        }
        if (load.isCompletedExceptionally()) {
            Throwable error = failure(load);
            RamLog.severe("player data load failed for " + entry.id, error);
            entry.ready.completeExceptionally(error);
            if (this.options.joinPolicy() == JoinPolicy.KICK) {
                this.entries.remove(entry.id, entry);
                kick(entry);
            }
            return;
        }
        if (entry.loadFuture() != load) {
            // a key registered during the load added another future; wait for that one too
            CompletableFuture<Void> latest = entry.loadFuture();
            latest.whenComplete((ignored, error) -> promote(entry, latest));
            return;
        }
        entry.loaded = true;
        entry.ready.complete(null);
    }

    private void kick(@NotNull Entry entry) {
        Player player = entry.player;
        if (player == null) {
            return;
        }
        Schedulers.run(TaskContext.of(player), () -> player.kick(this.options.kickMessage()));
    }

    @NotNull
    private static Throwable failure(@NotNull CompletableFuture<?> future) {
        try {
            future.join();
            return new IllegalStateException("load failed");
        } catch (Throwable thrown) {
            return thrown.getCause() != null ? thrown.getCause() : thrown;
        }
    }

    @NotNull
    @Override
    public Promise<Void> quit(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        Entry entry = this.entries.remove(playerId);
        if (entry == null) {
            return Promise.completed(null);
        }
        if (!entry.loaded) {
            entry.ready.completeExceptionally(new IllegalStateException("player quit before data loaded: " + playerId));
            return Promise.completed(null);
        }
        return Promise.wrapFuture(saveEntry(entry, entry.takeDirty(), false));
    }

    @Override
    public int sweepPending() {
        Instant cutoff = this.clock.instant().minus(this.options.loadTimeout().multipliedBy(4));
        int evicted = 0;
        for (Entry entry : this.entries.values()) {
            if (!entry.joined && !entry.loaded && entry.startedAt.isBefore(cutoff) && this.entries.remove(entry.id, entry)) {
                entry.ready.completeExceptionally(new IllegalStateException("login never completed for " + entry.id));
                evicted++;
            }
        }
        return evicted;
    }

    // ---- Service / Terminable ----

    @Override
    public void enable(@NotNull ServiceContext context) {
        RamPreconditions.checkState(!this.closed, "player data service is closed", "Create a new service; a closed one cannot be enabled again.");
        if (this.enabled) {
            return;
        }
        this.enabled = true;
        if (this.plugin != null) {
            this.plugin.registerListener(this.listener);
        }
        Duration autosave = this.options.autosaveInterval();
        if (!autosave.isZero()) {
            long ticks = Math.max(1L, Ticks.from(autosave.toMillis(), TimeUnit.MILLISECONDS));
            this.timers.add(Schedulers.runTimer(TaskContext.async(), ticks, ticks, this::autosave));
        }
        long sweepTicks = Math.max(1L, Ticks.from(this.options.loadTimeout().multipliedBy(4).toMillis(), TimeUnit.MILLISECONDS));
        this.timers.add(Schedulers.runTimer(TaskContext.async(), sweepTicks, sweepTicks, this::sweepPending));
        context.bind(this);
    }

    private void autosave() {
        saveDirty().exceptionallyAsync(error -> {
            RamLog.warn("player data autosave failed", error);
            return null;
        });
    }

    @Override
    public void disable(@NotNull ServiceContext context) {
        close();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        for (Task timer : this.timers) {
            timer.stop();
        }
        this.timers.clear();
        if (this.plugin != null && this.enabled) {
            HandlerList.unregisterAll(this.listener);
        }
        flushOnShutdown();
        this.entries.clear();
    }

    private void flushOnShutdown() {
        List<PendingWrite> writes = saveEverythingInline();
        if (writes.isEmpty()) {
            return;
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(writes.stream().map(PendingWrite::future).toArray(CompletableFuture[]::new));
        try {
            all.get(this.options.flushTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            RamLog.warn("player data flush timed out after " + this.options.flushTimeout().toMillis() + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            RamLog.warn("player data flush interrupted");
        } catch (Exception e) {
            // individual failures are logged by writeSnapshot; the summary below names them
        }
        for (PendingWrite write : writes) {
            if (!write.future.isDone()) {
                RamLog.warn("player data " + write.key + " for " + write.playerId + " did not flush before shutdown");
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @NotNull
    PlayerDataListener listener() {
        return this.listener;
    }

    // ---- state ----

    private record Registration<T>(@NotNull PlayerDataKey<T> key, @NotNull Store<UUID, T> store) {
    }

    private record PendingWrite(@NotNull UUID playerId, @NotNull PlayerDataKey<?> key, @NotNull CompletableFuture<Void> future) {
    }

    private static final class Entry {
        final UUID id;
        final Instant startedAt;
        final Map<PlayerDataKey<?>, Object> values = new ConcurrentHashMap<>();
        final Set<PlayerDataKey<?>> dirty = ConcurrentHashMap.newKeySet();
        final CompletableFuture<Void> ready = new CompletableFuture<>();
        private final List<CompletableFuture<Void>> loads = new ArrayList<>();
        private CompletableFuture<Void> composite;
        volatile Player player;
        volatile boolean joined;
        volatile boolean loaded;

        Entry(UUID id, Instant startedAt) {
            this.id = id;
            this.startedAt = startedAt;
        }

        synchronized void addLoad(CompletableFuture<Void> load) {
            this.loads.add(load);
            this.composite = null;
        }

        @NotNull
        synchronized CompletableFuture<Void> loadFuture() {
            if (this.composite == null) {
                this.composite = this.loads.isEmpty()
                        ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.allOf(this.loads.toArray(CompletableFuture[]::new));
            }
            return this.composite;
        }

        @NotNull
        Set<PlayerDataKey<?>> takeDirty() {
            Set<PlayerDataKey<?>> taken = new LinkedHashSet<>(this.dirty);
            // clear before the write so a mutation during the write re-dirties the key
            this.dirty.removeAll(taken);
            return taken;
        }
    }
}
