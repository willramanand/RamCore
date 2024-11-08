package dev.willram.ramcore.data;

import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public final class PDCs {

    public static <T, Z> Z get(PersistentDataHolder holder, String key, PersistentDataType<T, Z> type) {
        return holder.getPersistentDataContainer().get(NamespacedKeys.create(key), type);
    }

    public static <T, Z> void set(PersistentDataHolder holder, String key, PersistentDataType<T, Z> type, Z value) {
        holder.getPersistentDataContainer().set(NamespacedKeys.create(key), type, value);
    }

    public static boolean has(PersistentDataHolder holder, String key) {
        return holder.getPersistentDataContainer().has(NamespacedKeys.create(key));
    }

    private PDCs() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
