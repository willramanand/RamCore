package dev.willram.ramcore.store;

import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.store.sql.ConnectionProvider;
import dev.willram.ramcore.store.sql.SqlDialect;
import dev.willram.ramcore.store.sql.SqlStore;
import dev.willram.ramcore.store.sql.SqlStoreConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

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
     * A file-per-key store under a directory.
     *
     * @param directory the directory; created on first write
     * @param keyCodec  key to file name mapping
     * @param codec     value serialiser
     * @param <K>       key type
     * @param <V>       value type
     * @return the store
     */
    @NotNull
    public static <K, V> FileStore<K, V> file(@NotNull Path directory, @NotNull DataKeyCodec<K> keyCodec, @NotNull StoreCodec<V> codec) {
        return new FileStore<>(directory, keyCodec, codec, StoreMigrations.none());
    }

    /**
     * A file-per-key store under a directory, applying migrations on load.
     *
     * @param directory  the directory; created on first write
     * @param keyCodec   key to file name mapping
     * @param codec      value serialiser
     * @param migrations the migration chain
     * @param <K>        key type
     * @param <V>        value type
     * @return the store
     */
    @NotNull
    public static <K, V> FileStore<K, V> file(@NotNull Path directory, @NotNull DataKeyCodec<K> keyCodec, @NotNull StoreCodec<V> codec, @NotNull StoreMigrations<V> migrations) {
        return new FileStore<>(directory, keyCodec, codec, migrations);
    }

    /**
     * JSON files keyed by UUID, one per entry, using the Gson envelope codec.
     *
     * @param directory the directory
     * @param type      the value type
     * @param <V>       value type
     * @return the store
     */
    @NotNull
    public static <V> FileStore<UUID, V> jsonByUuid(@NotNull Path directory, @NotNull Class<V> type) {
        return file(directory, DataKeyCodec.uuidKeys(), StoreCodec.gson(type));
    }

    /**
     * JSON files keyed by string, one per entry, using the Gson envelope codec.
     *
     * @param directory the directory
     * @param type      the value type
     * @param <V>       value type
     * @return the store
     */
    @NotNull
    public static <V> FileStore<String, V> jsonByString(@NotNull Path directory, @NotNull Class<V> type) {
        return file(directory, DataKeyCodec.stringKeys(), StoreCodec.gson(type));
    }

    /**
     * A pooled SQL store (HikariCP) for the given connection settings. HikariCP and the JDBC driver
     * must be on the classpath (resolved by the plugin loader when {@code storage.sql} is enabled).
     *
     * @param config   connection settings
     * @param table    table name
     * @param keyCodec key to string mapping
     * @param codec    value serialiser
     * @param <K>      key type
     * @param <V>      value type
     * @return the store
     * @throws dev.willram.ramcore.exception.ApiMisuseException when HikariCP is unavailable
     */
    @NotNull
    public static <K, V> SqlStore<K, V> sql(@NotNull SqlStoreConfig config, @NotNull String table, @NotNull DataKeyCodec<K> keyCodec, @NotNull StoreCodec<V> codec) {
        return sql(config, table, keyCodec, codec, StoreMigrations.none());
    }

    /**
     * A pooled SQL store (HikariCP) applying migrations on load.
     *
     * @param config     connection settings
     * @param table      table name
     * @param keyCodec   key to string mapping
     * @param codec      value serialiser
     * @param migrations the migration chain
     * @param <K>        key type
     * @param <V>        value type
     * @return the store
     */
    @NotNull
    public static <K, V> SqlStore<K, V> sql(@NotNull SqlStoreConfig config, @NotNull String table, @NotNull DataKeyCodec<K> keyCodec, @NotNull StoreCodec<V> codec, @NotNull StoreMigrations<V> migrations) {
        Objects.requireNonNull(config, "config");
        return new SqlStore<>(ConnectionProvider.hikari(config), config.dialect(), table, keyCodec, codec, migrations);
    }

    /**
     * A SQL store over a caller-supplied connection provider (unpooled {@code driverManager}, a
     * custom pool, or a shared one).
     *
     * @param connections connection source; closed with the store
     * @param dialect     SQL dialect
     * @param table       table name
     * @param keyCodec    key to string mapping
     * @param codec       value serialiser
     * @param migrations  the migration chain
     * @param <K>         key type
     * @param <V>         value type
     * @return the store
     */
    @NotNull
    public static <K, V> SqlStore<K, V> sql(@NotNull ConnectionProvider connections, @NotNull SqlDialect dialect, @NotNull String table, @NotNull DataKeyCodec<K> keyCodec, @NotNull StoreCodec<V> codec, @NotNull StoreMigrations<V> migrations) {
        return new SqlStore<>(connections, dialect, table, keyCodec, codec, migrations);
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
