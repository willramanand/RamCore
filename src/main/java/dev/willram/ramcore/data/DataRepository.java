package dev.willram.ramcore.data;


import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DataRepository<K, V extends DataItem> {

    protected Map<K, V> registry;

    public DataRepository() {
        registry = new LinkedHashMap<>();
    }

    public abstract void setup();
    public abstract void saveAll();

    public V get(K k) {
        return registry.get(k);
    }

    public void add(K key, V value) {
        this.registry.put(key, value);
    }
    public void remove(K key) {
        this.registry.remove(key);
    }
    public boolean has(K key) {
        return registry.containsKey(key);
    }
    public Map<K, V> registry() {
        return registry;
    }
}
