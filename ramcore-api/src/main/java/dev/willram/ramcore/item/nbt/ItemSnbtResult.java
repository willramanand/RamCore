package dev.willram.ramcore.item.nbt;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Result from a safe item SNBT operation.
 */
public record ItemSnbtResult<T>(
        boolean supported,
        @NotNull Optional<T> value,
        @NotNull String message
) {
    public ItemSnbtResult {
        value = Objects.requireNonNull(value, "value");
        message = Objects.requireNonNull(message, "message");
    }

    @NotNull
    public static ItemSnbtResult<String> exported(@NotNull String snbt) {
        return new ItemSnbtResult<>(true, Optional.of(Objects.requireNonNull(snbt, "snbt")), "SNBT exported");
    }

    @NotNull
    public static ItemSnbtResult<ItemStack> imported(@NotNull ItemStack item) {
        return new ItemSnbtResult<>(true, Optional.of(Objects.requireNonNull(item, "item")), "SNBT imported");
    }

    @NotNull
    public static <T> ItemSnbtResult<T> unsupported(@NotNull String message) {
        return new ItemSnbtResult<>(false, Optional.empty(), message);
    }
}
