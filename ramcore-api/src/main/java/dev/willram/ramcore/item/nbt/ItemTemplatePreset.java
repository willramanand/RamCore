package dev.willram.ramcore.item.nbt;

import dev.willram.ramcore.item.ItemStackBuilder;
import dev.willram.ramcore.item.component.ItemComponentProfile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reusable item template preset.
 */
public final class ItemTemplatePreset {
    private final Material material;
    private final Consumer<ItemStackBuilder> customizer;

    private ItemTemplatePreset(Material material, Consumer<ItemStackBuilder> customizer) {
        this.material = Objects.requireNonNull(material, "material");
        this.customizer = Objects.requireNonNull(customizer, "customizer");
    }

    @NotNull
    public static ItemTemplatePreset of(@NotNull Material material, @NotNull Consumer<ItemStackBuilder> customizer) {
        return new ItemTemplatePreset(material, customizer);
    }

    @NotNull
    public ItemStack build() {
        ItemStackBuilder builder = ItemStackBuilder.of(this.material);
        this.customizer.accept(builder);
        return builder.build();
    }

    @NotNull
    public Material material() {
        return this.material;
    }

    @NotNull
    public ItemTemplatePreset withComponents(@NotNull ItemComponentProfile profile) {
        return new ItemTemplatePreset(this.material, this.customizer.andThen(builder -> builder.components(profile)));
    }
}
