package dev.willram.ramcore.playerdata;

import dev.willram.ramcore.store.AbstractAsyncStore;
import dev.willram.ramcore.store.StoreMigrations;
import dev.willram.ramcore.store.StoredRecord;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A store that runs on the async scheduler (so promises stay pending until
 * {@code FakeScheduler.runAsync()}) and records every backend call.
 */
final class AsyncMapStore<K, V> extends AbstractAsyncStore<K, V> {
    final Map<K, V> entries = new LinkedHashMap<>();
    final List<String> calls = new ArrayList<>();
    RuntimeException failLoadsWith;

    AsyncMapStore() {
        super(StoreMigrations.none());
    }

    @NotNull
    @Override
    protected Optional<StoredRecord<V>> doLoad(@NotNull K key) {
        this.calls.add("load:" + key);
        if (this.failLoadsWith != null) {
            throw this.failLoadsWith;
        }
        V value = this.entries.get(key);
        return value == null ? Optional.empty() : Optional.of(new StoredRecord<>(1, value));
    }

    @Override
    protected void doSave(@NotNull K key, @NotNull StoredRecord<V> record) {
        this.calls.add("save:" + key + "=" + record.value());
        this.entries.put(key, record.value());
    }

    @Override
    protected boolean doDelete(@NotNull K key) {
        this.calls.add("delete:" + key);
        return this.entries.remove(key) != null;
    }

    @NotNull
    @Override
    protected Set<K> doKeys() {
        return Set.copyOf(this.entries.keySet());
    }

    int saves() {
        return (int) this.calls.stream().filter(call -> call.startsWith("save:")).count();
    }
}
