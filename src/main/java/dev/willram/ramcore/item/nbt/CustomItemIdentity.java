package dev.willram.ramcore.item.nbt;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Namespaced logical identity for a custom item stack.
 */
public record CustomItemIdentity(
        @NotNull NamespacedKey key,
        int version
) {
    public CustomItemIdentity {
        key = Objects.requireNonNull(key, "key");
        if (version < 0) {
            throw new IllegalArgumentException("custom item identity version must not be negative");
        }
    }

    @NotNull
    public static CustomItemIdentity of(@NotNull NamespacedKey key) {
        return new CustomItemIdentity(key, 1);
    }

    @NotNull
    @Override
    public String toString() {
        return this.key + "@" + this.version;
    }
}
