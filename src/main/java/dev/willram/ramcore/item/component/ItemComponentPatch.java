package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ordered patch for item data-component mutations.
 */
public final class ItemComponentPatch {
    private final List<ItemComponentChange> changes;

    private ItemComponentPatch(List<ItemComponentChange> changes) {
        this.changes = List.copyOf(changes);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public static ItemComponentPatch diff(@NotNull ItemComponentSnapshot before, @NotNull ItemComponentSnapshot after) {
        Builder builder = builder();
        Set<DataComponentType> types = new LinkedHashSet<>();
        types.addAll(before.values().keySet());
        types.addAll(before.markers());
        types.addAll(after.values().keySet());
        types.addAll(after.markers());
        for (DataComponentType type : types) {
            boolean beforeHas = before.has(type);
            boolean afterHas = after.has(type);
            if (!afterHas && beforeHas) {
                builder.unset(type);
                continue;
            }
            if (!afterHas) {
                continue;
            }
            if (type instanceof DataComponentType.Valued<?> valued) {
                Object beforeValue = before.values().get(type);
                Object afterValue = after.values().get(type);
                if (!Objects.equals(beforeValue, afterValue)) {
                    setUntyped(builder, valued, afterValue);
                }
            } else if (!beforeHas && type instanceof DataComponentType.NonValued nonValued) {
                builder.set(nonValued);
            }
        }
        return builder.build();
    }

    @NotNull
    public List<ItemComponentChange> changes() {
        return this.changes;
    }

    public boolean empty() {
        return this.changes.isEmpty();
    }

    public void apply(@NotNull ItemComponentBackend backend) {
        Objects.requireNonNull(backend, "backend");
        for (ItemComponentChange change : this.changes) {
            switch (change.action()) {
                case SET -> set(backend, change.type(), change.value());
                case SET_MARKER -> backend.set((DataComponentType.NonValued) change.type());
                case UNSET -> backend.unset(change.type());
                case RESET -> backend.reset(change.type());
            }
        }
    }

    @NotNull
    public ItemComponentPatch filter(@NotNull Predicate<DataComponentType> predicate) {
        return new ItemComponentPatch(this.changes.stream().filter(change -> predicate.test(change.type())).toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(ItemComponentBackend backend, DataComponentType type, Object value) {
        backend.set((DataComponentType.Valued<T>) type, (T) value);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setUntyped(Builder builder, DataComponentType.Valued<?> type, Object value) {
        builder.set((DataComponentType.Valued<T>) type, (T) value);
    }

    public static final class Builder {
        private final List<ItemComponentChange> changes = new ArrayList<>();

        public <T> Builder set(@NotNull DataComponentType.Valued<T> type, @NotNull T value) {
            this.changes.add(new ItemComponentChange(type, ItemComponentAction.SET, Objects.requireNonNull(value, "value")));
            return this;
        }

        public <T> Builder set(@NotNull DataComponentType.Valued<T> type, @NotNull DataComponentBuilder<T> builder) {
            return set(type, Objects.requireNonNull(builder, "builder").build());
        }

        public Builder set(@NotNull DataComponentType.NonValued type) {
            this.changes.add(new ItemComponentChange(type, ItemComponentAction.SET_MARKER, null));
            return this;
        }

        public Builder unset(@NotNull DataComponentType type) {
            this.changes.add(new ItemComponentChange(type, ItemComponentAction.UNSET, null));
            return this;
        }

        public Builder reset(@NotNull DataComponentType type) {
            this.changes.add(new ItemComponentChange(type, ItemComponentAction.RESET, null));
            return this;
        }

        public ItemComponentPatch build() {
            return new ItemComponentPatch(this.changes);
        }
    }
}
