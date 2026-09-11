package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for backends whose I/O runs on the async scheduler.
 *
 * <p>Subclasses implement the blocking {@code do*} methods. This class serialises operations per
 * key (a save after a save on the same key runs strictly after it), runs them on
 * {@code Schedulers.async()}, applies {@link StoreMigrations} on load and writes migrated records
 * back inside the same serialised operation.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public abstract class AbstractAsyncStore<K, V> implements Store<K, V> {
    private static final Object ALL_KEYS_LANE = new Object();

    private final StoreMigrations<V> migrations;
    private final Map<Object, CompletableFuture<Void>> lanes = new HashMap<>();

    protected AbstractAsyncStore(@NotNull StoreMigrations<V> migrations) {
        this.migrations = Objects.requireNonNull(migrations, "migrations");
    }

    /**
     * The migrations applied on load.
     *
     * @return the migration chain
     */
    @NotNull
    public StoreMigrations<V> migrations() {
        return this.migrations;
    }

    // ---- blocking backend operations, run on an async thread ----

    @NotNull
    protected abstract Optional<StoredRecord<V>> doLoad(@NotNull K key) throws Exception;

    protected abstract void doSave(@NotNull K key, @NotNull StoredRecord<V> record) throws Exception;

    protected abstract boolean doDelete(@NotNull K key) throws Exception;

    @NotNull
    protected abstract Set<K> doKeys() throws Exception;

    /**
     * Loads every record. The default implementation lists keys and loads each; backends with a
     * cheaper bulk path should override.
     */
    @NotNull
    protected Map<K, StoredRecord<V>> doLoadAll() throws Exception {
        Map<K, StoredRecord<V>> result = new LinkedHashMap<>();
        for (K key : doKeys()) {
            doLoad(key).ifPresent(record -> result.put(key, record));
        }
        return result;
    }

    // ---- Store ----

    @NotNull
    @Override
    public Promise<Optional<V>> load(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        return serial(key, () -> {
            Optional<StoredRecord<V>> loaded = doLoad(key);
            if (loaded.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(migrateAndWriteBack(key, loaded.get()));
        });
    }

    @NotNull
    @Override
    public Promise<Void> save(@NotNull K key, @NotNull V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return serial(key, () -> {
            doSave(key, new StoredRecord<>(this.migrations.currentVersion(), value));
            return null;
        });
    }

    @NotNull
    @Override
    public Promise<Boolean> delete(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        return serial(key, () -> doDelete(key));
    }

    @NotNull
    @Override
    public Promise<Map<K, V>> loadAll() {
        return serial(ALL_KEYS_LANE, () -> {
            Map<K, V> result = new LinkedHashMap<>();
            for (Map.Entry<K, StoredRecord<V>> entry : doLoadAll().entrySet()) {
                result.put(entry.getKey(), migrateAndWriteBack(entry.getKey(), entry.getValue()));
            }
            return result;
        });
    }

    @NotNull
    @Override
    public Promise<Set<K>> keys() {
        return serial(ALL_KEYS_LANE, this::doKeys);
    }

    private V migrateAndWriteBack(K key, StoredRecord<V> record) throws Exception {
        StoredRecord<V> migrated = this.migrations.apply(record);
        if (migrated != record) {
            doSave(key, migrated);
        }
        return migrated.value();
    }

    /**
     * Runs an operation on the async scheduler, strictly after every earlier operation submitted
     * for the same lane.
     */
    @NotNull
    protected <T> Promise<T> serial(@NotNull Object lane, @NotNull Callable<T> operation) {
        Promise<T> promise = Promise.empty();
        CompletableFuture<Void> mine = new CompletableFuture<>();
        CompletableFuture<Void> previous;
        synchronized (this.lanes) {
            previous = this.lanes.getOrDefault(lane, CompletableFuture.completedFuture(null));
            this.lanes.put(lane, mine);
        }
        previous.whenComplete((ignored, error) -> Schedulers.async().execute(() -> {
            try {
                promise.supply(operation.call());
            } catch (Throwable t) {
                promise.supplyException(t instanceof StoreException ? t : new StoreException(describe(lane, t), t));
            } finally {
                mine.complete(null);
                synchronized (this.lanes) {
                    if (this.lanes.get(lane) == mine) {
                        this.lanes.remove(lane);
                    }
                }
            }
        }));
        return promise;
    }

    private String describe(Object lane, @Nullable Throwable t) {
        String target = lane == ALL_KEYS_LANE ? "all keys" : "key " + lane;
        return getClass().getSimpleName() + " operation failed for " + target + (t == null ? "" : ": " + t.getMessage());
    }
}
