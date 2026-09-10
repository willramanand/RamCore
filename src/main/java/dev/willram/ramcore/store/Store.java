package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.terminable.Terminable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Asynchronous keyed persistence.
 *
 * <p>A store is a transport: it loads and saves values by key and knows nothing about which values
 * are dirty. Wrap one in a {@link CachedStore} (via {@code Stores.cached(store)}) for an in-memory
 * working set with dirty tracking and batched saves.</p>
 *
 * <p>Threading: every method returns immediately. Backends run I/O on the async scheduler and
 * complete the promise there, so choose a {@code TaskContext} for continuations that touch server
 * state. Operations on the same key run in submission order; operations on different keys may
 * interleave. Migrations registered with the backend apply on load and are written back.</p>
 *
 * <p>Stability: stable. Folia-safe by design (no server state is touched).</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Store<K, V> extends Terminable {

    /**
     * Loads the value for a key.
     *
     * @param key the key
     * @return the value, or empty when absent
     */
    @NotNull
    Promise<Optional<V>> load(@NotNull K key);

    /**
     * Saves a value, replacing any existing one.
     *
     * @param key   the key
     * @param value the value
     * @return completes when the value is durable in this backend
     */
    @NotNull
    Promise<Void> save(@NotNull K key, @NotNull V value);

    /**
     * Deletes a key.
     *
     * @param key the key
     * @return true when a value was removed
     */
    @NotNull
    Promise<Boolean> delete(@NotNull K key);

    /**
     * Loads every entry. Prefer {@link #keys()} plus targeted loads for large stores.
     *
     * @return all entries
     */
    @NotNull
    Promise<Map<K, V>> loadAll();

    /**
     * Lists every key.
     *
     * @return all keys
     */
    @NotNull
    Promise<Set<K>> keys();

    /**
     * Releases backend resources (connection pools, file handles). Cached wrappers flush first.
     */
    @Override
    default void close() {
    }
}
