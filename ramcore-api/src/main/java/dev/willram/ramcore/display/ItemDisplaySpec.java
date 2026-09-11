package dev.willram.ramcore.display;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Item display entity configuration.
 */
public final class ItemDisplaySpec implements DisplaySpec<ItemDisplay> {
    private final DisplayOptions options = new DisplayOptions();
    private ItemStack itemStack;
    private ItemDisplay.ItemDisplayTransform transform;

    @NotNull
    public static ItemDisplaySpec item(@NotNull ItemStack itemStack) {
        return new ItemDisplaySpec().itemStack(itemStack);
    }

    @NotNull
    public ItemDisplaySpec itemStack(@NotNull ItemStack itemStack) {
        this.itemStack = requireNonNull(itemStack, "itemStack").clone();
        return this;
    }

    @NotNull
    public ItemDisplaySpec transform(@NotNull ItemDisplay.ItemDisplayTransform transform) {
        this.transform = transform;
        return this;
    }

    @NotNull
    public DisplayOptions options() {
        return this.options;
    }

    @NotNull
    @Override
    public Class<ItemDisplay> type() {
        return ItemDisplay.class;
    }

    @Override
    public void apply(@NotNull ItemDisplay display) {
        this.options.apply(display);
        if (this.itemStack != null) {
            display.setItemStack(this.itemStack.clone());
        }
        if (this.transform != null) {
            display.setItemDisplayTransform(this.transform);
        }
    }
}
