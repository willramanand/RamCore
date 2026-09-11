package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Immutable snapshot of item data components.
 */
public final class ItemComponentSnapshot {
    private final Map<DataComponentType, Object> values;
    private final Set<DataComponentType> markers;
    private final Set<DataComponentType> overridden;

    public ItemComponentSnapshot(@NotNull Map<DataComponentType, Object> values,
                                 @NotNull Set<DataComponentType> markers,
                                 @NotNull Set<DataComponentType> overridden) {
        this.values = Map.copyOf(Objects.requireNonNull(values, "values"));
        this.markers = Set.copyOf(Objects.requireNonNull(markers, "markers"));
        this.overridden = Set.copyOf(Objects.requireNonNull(overridden, "overridden"));
    }

    @NotNull
    public static ItemComponentSnapshot capture(@NotNull ItemComponentBackend backend) {
        Map<DataComponentType, Object> values = new LinkedHashMap<>();
        Set<DataComponentType> markers = new LinkedHashSet<>();
        Set<DataComponentType> overridden = new LinkedHashSet<>();
        for (DataComponentType type : backend.types()) {
            if (type instanceof DataComponentType.Valued<?> valued) {
                values.put(type, get(backend, valued));
            } else {
                markers.add(type);
            }
            if (backend.overridden(type)) {
                overridden.add(type);
            }
        }
        return new ItemComponentSnapshot(values, markers, overridden);
    }

    @NotNull
    public Map<DataComponentType, Object> values() {
        return this.values;
    }

    @NotNull
    public Set<DataComponentType> markers() {
        return this.markers;
    }

    @NotNull
    public Set<DataComponentType> overridden() {
        return this.overridden;
    }

    public boolean has(@NotNull DataComponentType type) {
        return this.markers.contains(type) || this.values.containsKey(type);
    }

    public boolean matches(@NotNull ItemComponentSnapshot other, @NotNull Set<DataComponentType> ignored, boolean ignorePrototype) {
        return filteredValues(ignored, ignorePrototype).equals(other.filteredValues(ignored, ignorePrototype))
                && filteredMarkers(ignored, ignorePrototype).equals(other.filteredMarkers(ignored, ignorePrototype));
    }

    @NotNull
    public ItemComponentPatch diff(@NotNull ItemComponentSnapshot after) {
        return ItemComponentPatch.diff(this, after);
    }

    void applyTo(@NotNull ItemComponentBackend target, @NotNull Predicate<DataComponentType> predicate) {
        for (Map.Entry<DataComponentType, Object> entry : this.values.entrySet()) {
            if (predicate.test(entry.getKey()) && entry.getKey() instanceof DataComponentType.Valued<?> valued) {
                set(target, valued, entry.getValue());
            }
        }
        for (DataComponentType type : this.markers) {
            if (predicate.test(type) && type instanceof DataComponentType.NonValued nonValued) {
                target.set(nonValued);
            }
        }
    }

    private Map<DataComponentType, Object> filteredValues(Set<DataComponentType> ignored, boolean ignorePrototype) {
        Map<DataComponentType, Object> filtered = new LinkedHashMap<>();
        this.values.forEach((type, value) -> {
            if (!ignored.contains(type) && (!ignorePrototype || this.overridden.contains(type))) {
                filtered.put(type, value);
            }
        });
        return filtered;
    }

    private Set<DataComponentType> filteredMarkers(Set<DataComponentType> ignored, boolean ignorePrototype) {
        Set<DataComponentType> filtered = new LinkedHashSet<>();
        this.markers.forEach(type -> {
            if (!ignored.contains(type) && (!ignorePrototype || this.overridden.contains(type))) {
                filtered.add(type);
            }
        });
        return filtered;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemComponentSnapshot that)) {
            return false;
        }
        return this.values.equals(that.values)
                && this.markers.equals(that.markers)
                && this.overridden.equals(that.overridden);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.values, this.markers, this.overridden);
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(ItemComponentBackend backend, DataComponentType.Valued<?> type) {
        return backend.get((DataComponentType.Valued<T>) type);
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(ItemComponentBackend backend, DataComponentType.Valued<?> type, Object value) {
        backend.set((DataComponentType.Valued<T>) type, (T) value);
    }
}
