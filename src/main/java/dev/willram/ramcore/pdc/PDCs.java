package dev.willram.ramcore.pdc;

import dev.willram.ramcore.data.NamespacedKeys;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public final class PDCs {

    @Nullable
    public static <T, Z> Z get(@NotNull PersistentDataHolder holder, @NotNull String key, @NotNull PersistentDataType<T, Z> type) {
        return holder.getPersistentDataContainer().get(NamespacedKeys.create(key), type);
    }

    public static <T, Z> void set(@NotNull PersistentDataHolder holder, @NotNull String key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        holder.getPersistentDataContainer().set(NamespacedKeys.create(key), type, value);
    }

    public static boolean has(@NotNull PersistentDataHolder holder, @NotNull String key) {
        return holder.getPersistentDataContainer().has(NamespacedKeys.create(key));
    }

    private PDCs() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
