package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test/offline backend for item data-component patches.
 */
public final class InMemoryItemComponentBackend implements ItemComponentBackend {
    private final Map<DataComponentType, Object> values = new LinkedHashMap<>();
    private final Set<DataComponentType> markers = new LinkedHashSet<>();
    private final Map<DataComponentType, Object> prototypeValues = new LinkedHashMap<>();
    private final Set<DataComponentType> prototypeMarkers = new LinkedHashSet<>();
    private final Set<DataComponentType> overridden = new LinkedHashSet<>();

    @NotNull
    public <T> InMemoryItemComponentBackend prototype(@NotNull DataComponentType.Valued<T> type, @NotNull T value) {
        this.prototypeValues.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(value, "value"));
        this.values.put(type, value);
        return this;
    }

    @NotNull
    public InMemoryItemComponentBackend prototype(@NotNull DataComponentType.NonValued type) {
        this.prototypeMarkers.add(Objects.requireNonNull(type, "type"));
        this.markers.add(type);
        return this;
    }

    @NotNull
    @Override
    public Set<DataComponentType> types() {
        Set<DataComponentType> types = new LinkedHashSet<>();
        types.addAll(this.values.keySet());
        types.addAll(this.markers);
        return types;
    }

    @Override
    public boolean has(@NotNull DataComponentType type) {
        return this.values.containsKey(type) || this.markers.contains(type);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull DataComponentType.Valued<T> type) {
        return (T) this.values.get(type);
    }

    @Override
    public <T> void set(@NotNull DataComponentType.Valued<T> type, @NotNull T value) {
        this.values.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(value, "value"));
        this.markers.remove(type);
        this.overridden.add(type);
    }

    @Override
    public <T> void set(@NotNull DataComponentType.Valued<T> type, @NotNull DataComponentBuilder<T> builder) {
        set(type, Objects.requireNonNull(builder, "builder").build());
    }

    @Override
    public void set(@NotNull DataComponentType.NonValued type) {
        this.markers.add(Objects.requireNonNull(type, "type"));
        this.values.remove(type);
        this.overridden.add(type);
    }

    @Override
    public void unset(@NotNull DataComponentType type) {
        this.values.remove(type);
        this.markers.remove(type);
        this.overridden.add(type);
    }

    @Override
    public void reset(@NotNull DataComponentType type) {
        this.values.remove(type);
        this.markers.remove(type);
        if (this.prototypeValues.containsKey(type)) {
            this.values.put(type, this.prototypeValues.get(type));
        }
        if (this.prototypeMarkers.contains(type)) {
            this.markers.add(type);
        }
        this.overridden.remove(type);
    }

    @Override
    public boolean overridden(@NotNull DataComponentType type) {
        return this.overridden.contains(type);
    }

    @NotNull
    @Override
    public ItemComponentSnapshot snapshot() {
        return ItemComponentSnapshot.capture(this);
    }

    @NotNull
    @Override
    public ItemComponentSnapshot prototypeSnapshot() {
        return new ItemComponentSnapshot(this.prototypeValues, this.prototypeMarkers, Set.of());
    }
}
