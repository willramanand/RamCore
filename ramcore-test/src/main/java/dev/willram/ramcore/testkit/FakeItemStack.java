package dev.willram.ramcore.testkit;

import org.bukkit.inventory.ItemStack;

/**
 * An {@link ItemStack} that can be constructed and cloned without a running server. Only useful
 * where the item is passed through opaquely (menus, buttons); it carries no material or meta.
 */
public final class FakeItemStack extends ItemStack {
    public FakeItemStack() {
        super();
    }

    @Override
    public ItemStack clone() {
        return new FakeItemStack();
    }
}
