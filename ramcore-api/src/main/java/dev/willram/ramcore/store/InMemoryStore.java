package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store backed by a concurrent map. Every promise completes synchronously on the caller's thread,
 * which makes it the default for tests and for consumers that do not need persistence.
 *
 * <p>Migrations apply on load like every other backend, so a migration chain can be tested
 * against this store by seeding old records with {@link #seed(Object, int, Object)}.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class InMemoryStore<K, V> implements Store<K, V> {
    private final Map<K, StoredRecord<V>> entries = new ConcurrentHashMap<>();
    private final StoreMigrations<V> migrations;

    public InMemoryStore() {
        this(StoreMigrations.none());
    }

    public InMemoryStore(@NotNull StoreMigrations<V> migrations) {
        this.migrations = Objects.requireNonNull(migrations, "migrations");
    }

    @NotNull
    public StoreMigrations<V> migrations() {
        return this.migrations;
    }

    /**
     * Inserts a record at an explicit version, bypassing migrations. For tests.
     *
     * @param key         the key
     * @param dataVersion the version to record
     * @param value       the value
     */
    public void seed(@NotNull K key, int dataVersion, @NotNull V value) {
        this.entries.put(Objects.requireNonNull(key, "key"), new StoredRecord<>(dataVersion, value));
    }

    /**
     * The stored version of a key, for asserting migration write-back.
     *
     * @param key the key
     * @return the version, or empty when absent
     */
    @NotNull
    public Optional<Integer> storedVersion(@NotNull K key) {
        return Optional.ofNullable(this.entries.get(key)).map(StoredRecord::dataVersion);
    }

    public int size() {
        return this.entries.size();
    }

    @NotNull
    @Override
    public Promise<Optional<V>> load(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        StoredRecord<V> record = this.entries.get(key);
        if (record == null) {
            return Promise.completed(Optional.empty());
        }
        StoredRecord<V> migrated = this.migrations.apply(record);
        if (migrated != record) {
            this.entries.put(key, migrated);
        }
        return Promise.completed(Optional.of(migrated.value()));
    }

    @NotNull
    @Override
    public Promise<Void> save(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        this.entries.put(key, new StoredRecord<>(this.migrations.currentVersion(), value));
        return Promise.completed(null);
    }

    @NotNull
    @Override
    public Promise<Boolean> delete(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        return Promise.completed(this.entries.remove(key) != null);
    }

    @NotNull
    @Override
    public Promise<Map<K, V>> loadAll() {
        Map<K, V> result = new LinkedHashMap<>();
        for (K key : Set.copyOf(this.entries.keySet())) {
            load(key).join().ifPresent(value -> result.put(key, value));
        }
        return Promise.completed(result);
    }

    @NotNull
    @Override
    public Promise<Set<K>> keys() {
        return Promise.completed(Set.copyOf(this.entries.keySet()));
    }
}
