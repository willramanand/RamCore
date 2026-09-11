package dev.willram.ramcore.item.nbt;

import dev.willram.ramcore.item.ItemStackBuilder;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Common item template presets for custom item libraries.
 */
public final class ItemTemplatePresets {

    @NotNull
    public static ItemTemplatePreset tool(@NotNull Material material) {
        return ItemTemplatePreset.of(material, builder -> builder.breakable(true).showAttributes());
    }

    @NotNull
    public static ItemTemplatePreset weapon(@NotNull Material material) {
        return ItemTemplatePreset.of(material, builder -> builder.breakable(true).showAttributes());
    }

    @NotNull
    public static ItemTemplatePreset armor(@NotNull Material material) {
        return ItemTemplatePreset.of(material, builder -> builder.breakable(true).showAttributes());
    }

    @NotNull
    public static ItemTemplatePreset consumable(@NotNull Material material) {
        return ItemTemplatePreset.of(material, builder -> builder.amount(1));
    }

    @NotNull
    public static ItemTemplatePreset key(@NotNull Material material) {
        return ItemTemplatePreset.of(material, ItemStackBuilder::hideAttributes);
    }

    @NotNull
    public static ItemTemplatePreset questItem(@NotNull Material material) {
        return ItemTemplatePreset.of(material, builder -> builder.hideAttributes().breakable(false));
    }

    @NotNull
    public static ItemTemplatePreset menuItem(@NotNull Material material) {
        return ItemTemplatePreset.of(material, ItemStackBuilder::hideAttributes);
    }

    @NotNull
    public static ItemTemplatePreset marker(@NotNull Material material) {
        return ItemTemplatePreset.of(material, builder -> builder.hideAttributes().breakable(false));
    }

    private ItemTemplatePresets() {
    }
}
