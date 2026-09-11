package dev.willram.ramcore.data;

import dev.willram.ramcore.utils.LoaderUtils;
import org.bukkit.NamespacedKey;

public final class NamespacedKeys {

    public static NamespacedKey create(String key) {
        return new NamespacedKey(LoaderUtils.getPlugin(), key);
    }

    private NamespacedKeys() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
