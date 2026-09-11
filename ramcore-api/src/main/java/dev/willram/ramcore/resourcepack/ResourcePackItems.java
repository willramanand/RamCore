package dev.willram.ramcore.resourcepack;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Applies resource-pack model keys to Bukkit items.
 */
public final class ResourcePackItems {

    @NotNull
    public static ItemStack apply(@NotNull ItemStack itemStack, @NotNull ResourcePackAsset asset) {
        ItemStack copy = requireNonNull(itemStack, "itemStack").clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            apply(meta, asset);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    public static void apply(@NotNull ItemMeta meta, @NotNull ResourcePackAsset asset) {
        requireNonNull(meta, "meta");
        requireNonNull(asset, "asset");
        if (asset.customModelData() != null) {
            meta.setCustomModelData(asset.customModelData());
        }
        if (asset.itemModelKey() != null) {
            meta.setItemModel(asset.itemModelKey().asBukkitKey());
        }
    }

    @NotNull
    public static ItemStack customModelData(@NotNull ItemStack itemStack, int customModelData) {
        return apply(itemStack, ResourcePackAsset.customModelData(ResourcePackAssetId.of("ramcore", "generated/custom_model_" + customModelData), customModelData));
    }

    @NotNull
    public static ItemStack itemModel(@NotNull ItemStack itemStack, @NotNull ResourcePackAssetId itemModelKey) {
        return apply(itemStack, ResourcePackAsset.itemModel(itemModelKey));
    }

    private ResourcePackItems() {
    }
}
