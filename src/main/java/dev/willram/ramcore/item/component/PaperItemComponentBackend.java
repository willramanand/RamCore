package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Item component backend using Paper's experimental ItemStack APIs.
 */
public final class PaperItemComponentBackend implements ItemComponentBackend {
    private final ItemStack item;

    public PaperItemComponentBackend(@NotNull ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    @NotNull
    public ItemStack item() {
        return this.item;
    }

    @NotNull
    @Override
    public Set<DataComponentType> types() {
        return this.item.getDataTypes();
    }

    @Override
    public boolean has(@NotNull DataComponentType type) {
        return this.item.hasData(Objects.requireNonNull(type, "type"));
    }

    @Nullable
    @Override
    public <T> T get(@NotNull DataComponentType.Valued<T> type) {
        return this.item.getData(Objects.requireNonNull(type, "type"));
    }

    @NotNull
    @Override
    public <T> T getOrDefault(@NotNull DataComponentType.Valued<? extends T> type, @NotNull T defaultValue) {
        return this.item.getDataOrDefault(type, defaultValue);
    }

    @Override
    public <T> void set(@NotNull DataComponentType.Valued<T> type, @NotNull T value) {
        this.item.setData(Objects.requireNonNull(type, "type"), Objects.requireNonNull(value, "value"));
    }

    @Override
    public <T> void set(@NotNull DataComponentType.Valued<T> type, @NotNull DataComponentBuilder<T> builder) {
        this.item.setData(Objects.requireNonNull(type, "type"), Objects.requireNonNull(builder, "builder"));
    }

    @Override
    public void set(@NotNull DataComponentType.NonValued type) {
        this.item.setData(Objects.requireNonNull(type, "type"));
    }

    @Override
    public void unset(@NotNull DataComponentType type) {
        this.item.unsetData(Objects.requireNonNull(type, "type"));
    }

    @Override
    public void reset(@NotNull DataComponentType type) {
        this.item.resetData(Objects.requireNonNull(type, "type"));
    }

    @Override
    public boolean overridden(@NotNull DataComponentType type) {
        return this.item.isDataOverridden(Objects.requireNonNull(type, "type"));
    }

    @NotNull
    @Override
    public ItemComponentSnapshot snapshot() {
        return ItemComponentSnapshot.capture(this);
    }

    @NotNull
    @Override
    public ItemComponentSnapshot prototypeSnapshot() {
        ItemStack clone = this.item.clone();
        for (DataComponentType type : Set.copyOf(clone.getDataTypes())) {
            clone.resetData(type);
        }
        return new PaperItemComponentBackend(clone).snapshot();
    }

    @Override
    public void copyFrom(@NotNull ItemComponentBackend source, @NotNull Predicate<DataComponentType> predicate) {
        if (source instanceof PaperItemComponentBackend paperSource) {
            this.item.copyDataFrom(paperSource.item, predicate);
            return;
        }
        ItemComponentBackend.super.copyFrom(source, predicate);
    }

    @Override
    public boolean matchesWithout(@NotNull ItemComponentBackend other, @NotNull Set<DataComponentType> ignored, boolean ignorePrototype) {
        if (other instanceof PaperItemComponentBackend paperOther) {
            return this.item.matchesWithoutData(paperOther.item, ignored, ignorePrototype);
        }
        return ItemComponentBackend.super.matchesWithout(other, ignored, ignorePrototype);
    }
}
