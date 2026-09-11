package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link Store} that delegates every call to another store. Domain stores (parties, cooldowns,
 * objective progress) extend this so any backend can be adapted to their typed interface.
 *
 * @param <K> key type
 * @param <V> value type
 */
public abstract class ForwardingStore<K, V> implements Store<K, V> {
    private final Store<K, V> delegate;

    protected ForwardingStore(@NotNull Store<K, V> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @NotNull
    public Store<K, V> delegate() {
        return this.delegate;
    }

    @NotNull
    @Override
    public Promise<Optional<V>> load(@NotNull K key) {
        return this.delegate.load(key);
    }

    @NotNull
    @Override
    public Promise<Void> save(@NotNull K key, @NotNull V value) {
        return this.delegate.save(key, value);
    }

    @NotNull
    @Override
    public Promise<Boolean> delete(@NotNull K key) {
        return this.delegate.delete(key);
    }

    @NotNull
    @Override
    public Promise<Map<K, V>> loadAll() {
        return this.delegate.loadAll();
    }

    @NotNull
    @Override
    public Promise<Set<K>> keys() {
        return this.delegate.keys();
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    @Override
    public boolean isClosed() {
        return this.delegate.isClosed();
    }
}
