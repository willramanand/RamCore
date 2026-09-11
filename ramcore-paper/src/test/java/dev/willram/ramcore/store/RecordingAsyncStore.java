package dev.willram.ramcore.store;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An {@link AbstractAsyncStore} over a plain map that records every backend call, for testing the
 * serialisation and migration machinery without I/O.
 */
final class RecordingAsyncStore<K, V> extends AbstractAsyncStore<K, V> {
    final Map<K, StoredRecord<V>> entries = new LinkedHashMap<>();
    final List<String> calls = new ArrayList<>();
    Consumer<K> beforeSave = key -> {
    };

    RecordingAsyncStore(StoreMigrations<V> migrations) {
        super(migrations);
    }

    @NotNull
    @Override
    protected Optional<StoredRecord<V>> doLoad(@NotNull K key) {
        this.calls.add("load:" + key);
        return Optional.ofNullable(this.entries.get(key));
    }

    @Override
    protected void doSave(@NotNull K key, @NotNull StoredRecord<V> record) {
        this.beforeSave.accept(key);
        this.calls.add("save:" + key + "=" + record.value());
        this.entries.put(key, record);
    }

    @Override
    protected boolean doDelete(@NotNull K key) {
        this.calls.add("delete:" + key);
        return this.entries.remove(key) != null;
    }

    @NotNull
    @Override
    protected Set<K> doKeys() {
        this.calls.add("keys");
        return Set.copyOf(this.entries.keySet());
    }
}
