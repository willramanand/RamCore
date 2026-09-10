package dev.willram.ramcore.data;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @deprecated since 2.1. Use {@code dev.willram.ramcore.store.CachedStore} via {@code Stores.cached(..)}:
 * async, per-key ordered, with dirty tracking and migrations for any value type.
 */
@Deprecated(since = "2.1")
public abstract class DataRepository<K, V extends DataItem> {

    protected Map<K, V> registry;

    public DataRepository() {
        registry = new LinkedHashMap<>();
    }

    public abstract void setup();
    public abstract void saveAll();

    @Nullable
    public V get(K key) {
        return registry.get(key);
    }

    @NotNull
    public Optional<V> find(K key) {
        return Optional.ofNullable(get(key));
    }

    @NotNull
    public V require(K key) {
        V value = get(key);
        if (value == null) {
            throw new IllegalArgumentException("Data item not found: " + key);
        }
        return value;
    }

    public void add(K key, V value) {
        this.registry.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    public void remove(K key) {
        this.registry.remove(key);
    }

    public boolean has(K key) {
        return registry.containsKey(key);
    }

    public int size() {
        return this.registry.size();
    }

    @NotNull
    public Collection<V> values() {
        return this.registry.values();
    }

    public Map<K, V> registry() {
        return registry;
    }
}
