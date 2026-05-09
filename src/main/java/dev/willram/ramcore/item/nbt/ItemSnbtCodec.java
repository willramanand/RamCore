package dev.willram.ramcore.item.nbt;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Safe item SNBT import/export contract.
 *
 * <p>Bukkit/Paper does not expose a stable raw item SNBT API. Implementations
 * that use NMS should stay behind this contract and report capability support
 * explicitly.</p>
 */
public interface ItemSnbtCodec {

    @NotNull
    ItemSnbtResult<String> exportSnbt(@NotNull ItemStack item);

    @NotNull
    ItemSnbtResult<ItemStack> importSnbt(@NotNull String snbt);

    boolean supported();
}
