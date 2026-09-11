package dev.willram.ramcore.item.nbt;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Explicit unsupported SNBT codec for environments without a guarded NMS adapter.
 */
public final class UnsupportedItemSnbtCodec implements ItemSnbtCodec {
    private final String reason;

    public UnsupportedItemSnbtCodec(@NotNull String reason) {
        this.reason = reason;
    }

    @NotNull
    @Override
    public ItemSnbtResult<String> exportSnbt(@NotNull ItemStack item) {
        return ItemSnbtResult.unsupported(this.reason);
    }

    @NotNull
    @Override
    public ItemSnbtResult<ItemStack> importSnbt(@NotNull String snbt) {
        return ItemSnbtResult.unsupported(this.reason);
    }

    @Override
    public boolean supported() {
        return false;
    }
}
