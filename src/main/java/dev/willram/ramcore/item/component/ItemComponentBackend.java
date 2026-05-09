package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Backend abstraction over Paper's experimental data-component methods.
 */
public interface ItemComponentBackend {

    @NotNull
    Set<DataComponentType> types();

    boolean has(@NotNull DataComponentType type);

    @Nullable
    <T> T get(@NotNull DataComponentType.Valued<T> type);

    @NotNull
    default <T> T getOrDefault(@NotNull DataComponentType.Valued<? extends T> type, @NotNull T defaultValue) {
        T value = get(cast(type));
        return value == null ? defaultValue : value;
    }

    <T> void set(@NotNull DataComponentType.Valued<T> type, @NotNull T value);

    <T> void set(@NotNull DataComponentType.Valued<T> type, @NotNull DataComponentBuilder<T> builder);

    void set(@NotNull DataComponentType.NonValued type);

    void unset(@NotNull DataComponentType type);

    void reset(@NotNull DataComponentType type);

    boolean overridden(@NotNull DataComponentType type);

    @NotNull
    ItemComponentSnapshot snapshot();

    @NotNull
    ItemComponentSnapshot prototypeSnapshot();

    default void copyFrom(@NotNull ItemComponentBackend source, @NotNull Predicate<DataComponentType> predicate) {
        source.snapshot().applyTo(this, predicate);
    }

    default boolean matchesWithout(@NotNull ItemComponentBackend other, @NotNull Set<DataComponentType> ignored, boolean ignorePrototype) {
        return snapshot().matches(other.snapshot(), ignored, ignorePrototype);
    }

    @SuppressWarnings("unchecked")
    private static <T> DataComponentType.Valued<T> cast(DataComponentType.Valued<? extends T> type) {
        return (DataComponentType.Valued<T>) type;
    }
}
