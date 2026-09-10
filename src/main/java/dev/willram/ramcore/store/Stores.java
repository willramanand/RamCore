package dev.willram.ramcore.store;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

/**
 * Factory facade for stores.
 *
 * <pre>{@code
 * CachedStore<UUID, Profile> profiles = Stores.cached(Stores.inMemory());
 * profiles.put(uuid, profile);           // cache + dirty, no I/O
 * profiles.saveDirty();                  // writes through, async
 * plugin.bind(profiles);                 // flushes on disable
 * }</pre>
 */
public final class Stores {
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(30);

    private Stores() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * An in-memory store with no migrations.
     *
     * @param <K> key type
     * @param <V> value type
     * @return the store
     */
    @NotNull
    public static <K, V> InMemoryStore<K, V> inMemory() {
        return new InMemoryStore<>();
    }

    /**
     * An in-memory store applying the given migrations on load.
     *
     * @param migrations the migration chain
     * @param <K>        key type
     * @param <V>        value type
     * @return the store
     */
    @NotNull
    public static <K, V> InMemoryStore<K, V> inMemory(@NotNull StoreMigrations<V> migrations) {
        return new InMemoryStore<>(migrations);
    }

    /**
     * Wraps a backend with an in-memory working set and dirty tracking. Closing waits up to 30
     * seconds for the final flush.
     *
     * @param backing the backend
     * @param <K>     key type
     * @param <V>     value type
     * @return the cached store
     */
    @NotNull
    public static <K, V> CachedStore<K, V> cached(@NotNull Store<K, V> backing) {
        return cached(backing, DEFAULT_CLOSE_TIMEOUT);
    }

    /**
     * Wraps a backend with an in-memory working set and dirty tracking.
     *
     * @param backing      the backend
     * @param closeTimeout how long {@code close()} waits for the final flush
     * @param <K>          key type
     * @param <V>          value type
     * @return the cached store
     */
    @NotNull
    public static <K, V> CachedStore<K, V> cached(@NotNull Store<K, V> backing, @NotNull Duration closeTimeout) {
        Objects.requireNonNull(backing, "backing");
        return new SimpleCachedStore<>(backing, closeTimeout);
    }
}
