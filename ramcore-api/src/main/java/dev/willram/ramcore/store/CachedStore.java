package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link Store} with an in-memory working set and {@link DirtyTracking}.
 *
 * <p>Reads after {@link #load(Object)} or {@link #loadAll()} are synchronous through
 * {@link #cached(Object)} and {@link #require(Object)}. Writes go through {@link #put(Object, Object)}
 * (cache + dirty mark, no I/O) and are persisted by {@link #saveDirty()}, or through
 * {@link #save(Object, Object)} which writes through immediately.</p>
 *
 * <p>{@link #close()} flushes dirty entries and waits, bounded, for the backend to finish, so
 * binding a cached store to a plugin persists it on disable.</p>
 *
 * <p>Stability: stable. Folia-safe by design.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface CachedStore<K, V> extends Store<K, V>, DirtyTracking<K> {

    /**
     * The backend this cache writes through to.
     *
     * @return the backing store
     */
    @NotNull
    Store<K, V> backing();

    /**
     * Synchronous read of a cached value. Does not touch the backend.
     *
     * @param key the key
     * @return the cached value, or empty when not loaded
     */
    @NotNull
    Optional<V> cached(@NotNull K key);

    /**
     * Synchronous read of a cached value that must be present.
     *
     * @param key the key
     * @return the cached value
     * @throws dev.willram.ramcore.exception.ApiMisuseException when the key is not cached
     */
    @NotNull
    V require(@NotNull K key);

    /**
     * Puts a value in the cache and marks it dirty. No I/O happens until {@link #saveDirty()}.
     *
     * @param key   the key
     * @param value the value
     */
    void put(@NotNull K key, @NotNull V value);

    /**
     * Removes a key from the cache (and its dirty mark) without touching the backend.
     *
     * @param key the key
     * @return true when the key was cached
     */
    boolean evict(@NotNull K key);

    /**
     * Snapshot of every cached entry.
     *
     * @return cached entries
     */
    @NotNull
    Map<K, V> cachedEntries();

    /**
     * Saves every dirty entry; alias of {@link #saveDirty()} that reads well at shutdown.
     *
     * @return number of entries saved
     */
    @NotNull
    default Promise<Integer> flush() {
        return saveDirty();
    }
}
