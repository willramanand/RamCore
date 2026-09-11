package dev.willram.ramcore.menu;

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


import com.google.common.collect.ImmutableMap;

import dev.willram.ramcore.interfaces.Delegates;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The initial model of a clickable item in a {@link Gui}. Immutable.
 */
public class Item {

    @NotNull
    public static Item.Builder builder(@NotNull ItemStack itemStack) {
        return new Builder(itemStack);
    }

    // the click handlers for this item
    private final Map<ClickType, Consumer<InventoryClickEvent>> handlers;
    // the backing itemstack
    private final ItemStack itemStack;

    public Item(@NotNull Map<ClickType, Consumer<InventoryClickEvent>> handlers, @NotNull ItemStack itemStack) {
        this.handlers = ImmutableMap.copyOf(Objects.requireNonNull(handlers, "handlers"));
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack").clone();
    }

    /**
     * Gets the click handlers for this Item.
     *
     * @return the click handlers
     */
    @NotNull
    public Map<ClickType, Consumer<InventoryClickEvent>> getHandlers() {
        return this.handlers;
    }

    /**
     * Gets the ItemStack backing this Item.
     *
     * @return the backing itemstack
     */
    @NotNull
    public ItemStack getItemStack() {
        return this.itemStack.clone();
    }

    @NotNull
    public MenuButton toButton() {
        return MenuButton.fromItem(this);
    }

    /**
     * Aids creation of {@link Item} instances.
     */
    public static final class Builder {
        private final ItemStack itemStack;
        private final Map<ClickType, Consumer<InventoryClickEvent>> handlers;

        private Builder(@NotNull ItemStack itemStack) {
            this.itemStack = Objects.requireNonNull(itemStack, "itemStack").clone();
            this.handlers = new HashMap<>();
        }

        @NotNull
        public Builder bind(@NotNull ClickType type, @Nullable Consumer<InventoryClickEvent> handler) {
            Objects.requireNonNull(type, "type");
            if (handler != null) {
                this.handlers.put(type, handler);
            } else {
                this.handlers.remove(type);
            }
            return this;
        }

        @NotNull
        public Builder bind(@NotNull ClickType type, @Nullable Runnable handler) {
            Objects.requireNonNull(type, "type");
            if (handler != null) {
                this.handlers.put(type, transformRunnable(handler));
            } else {
                this.handlers.remove(type);
            }
            return this;
        }

        @NotNull
        public Builder bind(@Nullable Consumer<InventoryClickEvent> handler, @NotNull ClickType... types) {
            for (ClickType type : types) {
                bind(type, handler);
            }
            return this;
        }

        @NotNull
        public Builder bind(@Nullable Runnable handler, @NotNull ClickType... types) {
            for (ClickType type : types) {
                bind(type, handler);
            }
            return this;
        }

        @NotNull
        public <T extends Runnable> Builder bindAllRunnables(@NotNull Iterable<Map.Entry<ClickType, T>> handlers) {
            Objects.requireNonNull(handlers, "handlers");
            for (Map.Entry<ClickType, T> handler : handlers) {
                bind(handler.getKey(), handler.getValue());
            }
            return this;
        }

        @NotNull
        public <T extends Consumer<InventoryClickEvent>> Builder bindAllConsumers(@NotNull Iterable<Map.Entry<ClickType, T>> handlers) {
            Objects.requireNonNull(handlers, "handlers");
            for (Map.Entry<ClickType, T> handler : handlers) {
                bind(handler.getKey(), handler.getValue());
            }
            return this;
        }

        @NotNull
        public Item build() {
            return new Item(this.handlers, this.itemStack);
        }
    }

    @NotNull
    public static Consumer<InventoryClickEvent> transformRunnable(@NotNull Runnable runnable) {
        return Delegates.runnableToConsumer(runnable);
    }
}
