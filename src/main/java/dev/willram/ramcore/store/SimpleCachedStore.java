package dev.willram.ramcore.store;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class SimpleCachedStore<K, V> implements CachedStore<K, V> {
    private final Store<K, V> backing;
    private final Duration closeTimeout;
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Set<K> dirty = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    SimpleCachedStore(@NotNull Store<K, V> backing, @NotNull Duration closeTimeout) {
        this.backing = Objects.requireNonNull(backing, "backing");
        this.closeTimeout = Objects.requireNonNull(closeTimeout, "closeTimeout");
    }

    @NotNull
    @Override
    public Store<K, V> backing() {
        return this.backing;
    }

    @NotNull
    @Override
    public Optional<V> cached(@NotNull K key) {
        return Optional.ofNullable(this.cache.get(Objects.requireNonNull(key, "key")));
    }

    @NotNull
    @Override
    public V require(@NotNull K key) {
        V value = this.cache.get(Objects.requireNonNull(key, "key"));
        if (value == null) {
            throw RamPreconditions.misuse("no cached value for key " + key,
                    "Call load(key) or loadAll() and wait for the promise before require(key), or use cached(key).");
        }
        return value;
    }

    @Override
    public void put(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        this.cache.put(key, value);
        this.dirty.add(key);
    }

    @Override
    public boolean evict(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        this.dirty.remove(key);
        return this.cache.remove(key) != null;
    }

    @NotNull
    @Override
    public Map<K, V> cachedEntries() {
        return Map.copyOf(this.cache);
    }

    // ---- DirtyTracking ----

    @Override
    public void markDirty(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        RamPreconditions.checkArgument(this.cache.containsKey(key),
                "cannot mark uncached key dirty: " + key,
                "put(key, value) the value first; markDirty is for values already in the cache.");
        this.dirty.add(key);
    }

    @Override
    public boolean dirty(@NotNull K key) {
        return this.dirty.contains(Objects.requireNonNull(key, "key"));
    }

    @NotNull
    @Override
    public Set<K> dirtyKeys() {
        return Set.copyOf(this.dirty);
    }

    @Override
    public void clearDirty(@NotNull K key) {
        this.dirty.remove(Objects.requireNonNull(key, "key"));
    }

    @NotNull
    @Override
    public Promise<Integer> saveDirty() {
        List<K> keys = new ArrayList<>(this.dirty);
        if (keys.isEmpty()) {
            return Promise.completed(0);
        }
        List<CompletableFuture<Void>> writes = new ArrayList<>(keys.size());
        for (K key : keys) {
            V value = this.cache.get(key);
            if (value == null) {
                this.dirty.remove(key);
                continue;
            }
            // clear before the write so a put() during the write re-dirties the key
            this.dirty.remove(key);
            writes.add(this.backing.save(key, value).toCompletableFuture());
        }
        int count = writes.size();
        return Promise.wrapFuture(CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenApply(ignored -> count));
    }

    // ---- Store ----

    @NotNull
    @Override
    public Promise<Optional<V>> load(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        V cached = this.cache.get(key);
        if (cached != null) {
            return Promise.completed(Optional.of(cached));
        }
        // cache bookkeeping is pure, so it runs inline on whichever thread completes the backend
        return Promise.wrapFuture(this.backing.load(key).toCompletableFuture().thenApply(loaded -> {
            loaded.ifPresent(value -> this.cache.putIfAbsent(key, value));
            return Optional.ofNullable(this.cache.get(key));
        }));
    }

    @NotNull
    @Override
    public Promise<Void> save(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        this.cache.put(key, value);
        this.dirty.remove(key);
        return this.backing.save(key, value);
    }

    @NotNull
    @Override
    public Promise<Boolean> delete(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        evict(key);
        return this.backing.delete(key);
    }

    @NotNull
    @Override
    public Promise<Map<K, V>> loadAll() {
        return Promise.wrapFuture(this.backing.loadAll().toCompletableFuture().thenApply(loaded -> {
            loaded.forEach(this.cache::putIfAbsent);
            return new LinkedHashMap<>(this.cache);
        }));
    }

    @NotNull
    @Override
    public Promise<Set<K>> keys() {
        return Promise.wrapFuture(this.backing.keys().toCompletableFuture().thenApply(keys -> {
            Set<K> union = new java.util.HashSet<>(keys);
            union.addAll(this.cache.keySet());
            return Set.copyOf(union);
        }));
    }

    /**
     * Flushes dirty entries and waits up to the close timeout, then closes the backend. Bounded so
     * a wedged backend cannot hang plugin disable forever; a timeout is logged, not thrown.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            if (!this.dirty.isEmpty()) {
                saveDirty().toCompletableFuture().get(this.closeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            RamLog.warn("cached store flush timed out after " + this.closeTimeout.toMillis() + "ms; some entries may not have been saved");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            RamLog.warn("cached store flush interrupted; some entries may not have been saved");
        } catch (Exception e) {
            RamLog.severe("cached store flush failed", e);
        } finally {
            try {
                this.backing.close();
            } catch (Exception e) {
                RamLog.severe("failed to close backing store", e);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
