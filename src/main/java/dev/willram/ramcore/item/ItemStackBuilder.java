package dev.willram.ramcore.item;

/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

import dev.willram.ramcore.menu.*;
import dev.willram.ramcore.pdc.PDCs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Easily construct {@link ItemStack} instances
 */
@Nonnull
public final class ItemStackBuilder {
    private static final ItemFlag[] ALL_FLAGS = new ItemFlag[]{
            ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS,
            ItemFlag.HIDE_PLACED_ON
    };

    private final ItemStack itemStack;

    public static ItemStackBuilder of(Material material) {
        return new ItemStackBuilder(new ItemStack(material)).hideAttributes();
    }

    public static ItemStackBuilder of(ItemStack itemStack) {
        return new ItemStackBuilder(itemStack).hideAttributes();
    }

//    public static ItemStackBuilder of(ConfigurationSection config) {
//        return ItemStackReader.DEFAULT.read(config);
//    }

    private ItemStackBuilder(ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
    }

    public ItemStackBuilder transform(Consumer<ItemStack> is) {
        is.accept(this.itemStack);
        return this;
    }

    public ItemStackBuilder transformMeta(Consumer<ItemMeta> meta) {
        ItemMeta m = this.itemStack.getItemMeta();
        if (m != null) {
            meta.accept(m);
            this.itemStack.setItemMeta(m);
        }
        return this;
    }
    public ItemStackBuilder name(Component name) {
        return transformMeta(meta -> meta.displayName(name.decoration(TextDecoration.ITALIC, false)));
    }

    public ItemStackBuilder name(String name) {
        return transformMeta(meta -> meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false)));
    }

    public ItemStackBuilder lore(String line) {
        return transformMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : meta.lore();
            lore.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });
    }

    public ItemStackBuilder lore(Component... lines) {
        return transformMeta(meta -> {
            List<Component> newLore = new ArrayList<>();
            for (Component line : lines) {
                newLore.add(line.decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(newLore);
        });
    }

    public ItemStackBuilder lore(String... lines) {
        return transformMeta(meta -> {
            List<Component> newLore = new ArrayList<>();
            for (String line : lines) {
                newLore.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(newLore);
        });
    }

    public ItemStackBuilder lore(Iterable<Component> lines) {
        return transformMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : meta.lore();
            for (Component line : lines) {
                lore.add(line.decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        });
    }

    public ItemStackBuilder clearLore() {
        return transformMeta(meta -> meta.lore(new ArrayList<>()));
    }

    public ItemStackBuilder durability(int durability) {
        return transformMeta(meta -> {
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(durability);
            }
        });
    }

    public ItemStackBuilder data(int data) {
        return durability(data);
    }

    public ItemStackBuilder amount(int amount) {
        return transform(itemStack -> itemStack.setAmount(amount));
    }

    public ItemStackBuilder enchant(Enchantment enchantment, int level) {
        return transform(itemStack -> itemStack.addUnsafeEnchantment(enchantment, level));
    }

    public ItemStackBuilder enchant(Enchantment enchantment) {
        return transform(itemStack -> itemStack.addUnsafeEnchantment(enchantment, 1));
    }

    public ItemStackBuilder clearEnchantments() {
        return transform(itemStack -> itemStack.getEnchantments().keySet().forEach(itemStack::removeEnchantment));
    }

    public ItemStackBuilder flag(ItemFlag... flags) {
        return transformMeta(meta -> meta.addItemFlags(flags));
    }

    public ItemStackBuilder unflag(ItemFlag... flags) {
        return transformMeta(meta -> meta.removeItemFlags(flags));
    }

    public ItemStackBuilder hideAttributes() {
        return flag(ALL_FLAGS);
    }

    public ItemStackBuilder showAttributes() {
        return unflag(ALL_FLAGS);
    }

    public ItemStackBuilder color(Color color) {
        return transform(itemStack -> {
            Material type = itemStack.getType();
            if (type == Material.LEATHER_BOOTS || type == Material.LEATHER_CHESTPLATE || type == Material.LEATHER_HELMET || type == Material.LEATHER_LEGGINGS) {
                LeatherArmorMeta meta = (LeatherArmorMeta) itemStack.getItemMeta();
                meta.setColor(color);
                itemStack.setItemMeta(meta);
            }
        });
    }

    public ItemStackBuilder breakable(boolean flag) {
        return transformMeta(meta -> meta.setUnbreakable(!flag));
    }

    public ItemStackBuilder apply(Consumer<ItemStackBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    public ItemStack build() {
        return this.itemStack;
    }

    public Item.Builder buildItem() {
        transformMeta(meta ->
                PDCs.set(meta, Gui.GUI_ITEM_KEY, PersistentDataType.BOOLEAN, true)
        );
        return Item.builder(build());
    }

    public Item build(@Nullable Runnable handler) {
        return buildItem().bind(handler, ClickType.RIGHT, ClickType.LEFT).build();
    }

    public Item build(ClickType type, @Nullable Runnable handler) {
        return buildItem().bind(type, handler).build();
    }

    public Item build(@Nullable Runnable rightClick, @Nullable Runnable leftClick) {
        return buildItem().bind(ClickType.RIGHT, rightClick).bind(ClickType.LEFT, leftClick).build();
    }

    public Item buildFromMap(Map<ClickType, Runnable> handlers) {
        return buildItem().bindAllRunnables(handlers.entrySet()).build();
    }

    public Item buildConsumer(@Nullable Consumer<InventoryClickEvent> handler) {
        return buildItem().bind(handler, ClickType.RIGHT, ClickType.LEFT).build();
    }

    public Item buildConsumer(ClickType type, @Nullable Consumer<InventoryClickEvent> handler) {
        return buildItem().bind(type, handler).build();
    }

    public Item buildConsumer(@Nullable Consumer<InventoryClickEvent> rightClick, @Nullable Consumer<InventoryClickEvent> leftClick) {
        return buildItem().bind(ClickType.RIGHT, rightClick).bind(ClickType.LEFT, leftClick).build();
    }

    public Item buildFromConsumerMap(Map<ClickType, Consumer<InventoryClickEvent>> handlers) {
        return buildItem().bindAllConsumers(handlers.entrySet()).build();
    }

}