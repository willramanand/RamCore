package dev.willram.ramcore.item.nbt;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Static entry point for item NBT, identity, snapshot, and serialization helpers.
 */
public final class ItemNbt {
    private static final ItemSnbtCodec UNSUPPORTED_SNBT = new UnsupportedItemSnbtCodec("Raw item SNBT requires a guarded NMS adapter.");
    private static final BukkitItemBinaryCodec BINARY = new BukkitItemBinaryCodec();

    @NotNull
    public static ItemSnbtCodec unsupportedSnbtCodec() {
        return UNSUPPORTED_SNBT;
    }

    @NotNull
    public static BukkitItemBinaryCodec binaryCodec() {
        return BINARY;
    }

    @NotNull
    public static ItemSnapshot snapshot(@NotNull ItemStack item) {
        return ItemSnapshot.capture(item);
    }

    @NotNull
    public static ItemSnapshot snapshot(@NotNull ItemStack item, @NotNull ItemSnbtCodec snbtCodec) {
        return ItemSnapshot.capture(item, snbtCodec);
    }

    @NotNull
    public static ItemDiff diff(@NotNull ItemSnapshot before, @NotNull ItemSnapshot after) {
        return ItemDiff.between(before, after);
    }

    @NotNull
    public static ItemStack identify(@NotNull ItemStack item, @NotNull CustomItemIdentity identity) {
        return CustomItemIdentityStore.apply(item, identity);
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.ITEM_NBT,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper-item-serialization",
                null,
                null,
                "Bukkit/Paper exposes safe binary item serialization, meta/PDC/components, and debug snapshots; raw SNBT import/export requires a guarded NMS adapter."
        ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.ITEM_BINARY_SERIALIZATION,
                        NmsAccessTier.PAPER_API,
                        "paper-item-serialization",
                        "Bukkit/Paper binary ItemStack serialization is available."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.ITEM_SNBT,
                        NmsAccessTier.GUARDED_REFLECTION,
                        "none",
                        "Raw item SNBT import/export requires a guarded NMS adapter."
                ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.ITEM_DIFFS,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-item-snapshots",
                        "RamCore compares item type, amount, meta, PDC, components, enchantments, attributes, and optional raw SNBT."
                ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.CUSTOM_ITEM_IDENTITY,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-item-identity",
                        "RamCore stores namespaced custom item identity through public item metadata/PDC surfaces."
                ));
        return registry;
    }

    private ItemNbt() {
    }
}
