package dev.willram.ramcore.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Declarative clickable button rendered in a menu slot.
 */
public final class MenuButton {
    private final ItemStack itemStack;
    private final Map<ClickType, MenuAction> handlers;
    private final MenuAction fallbackHandler;
    private final boolean closeAfterClick;
    private final boolean refreshAfterClick;

    private MenuButton(
            @NotNull ItemStack itemStack,
            @NotNull Map<ClickType, MenuAction> handlers,
            @Nullable MenuAction fallbackHandler,
            boolean closeAfterClick,
            boolean refreshAfterClick
    ) {
        this.itemStack = requireNonNull(itemStack, "itemStack").clone();
        this.handlers = Map.copyOf(handlers);
        this.fallbackHandler = fallbackHandler;
        this.closeAfterClick = closeAfterClick;
        this.refreshAfterClick = refreshAfterClick;
    }

    @NotNull
    public static Builder builder(@NotNull ItemStack itemStack) {
        return new Builder(itemStack);
    }

    @NotNull
    public static MenuButton of(@NotNull ItemStack itemStack) {
        return builder(itemStack).build();
    }

    @NotNull
    public static MenuButton fromItem(@NotNull Item item) {
        requireNonNull(item, "item");
        Builder builder = builder(item.getItemStack());
        item.getHandlers().forEach((type, handler) -> builder.on(type, context -> handler.accept(context.event())));
        return builder.build();
    }

    @NotNull
    public ItemStack itemStack() {
        return this.itemStack.clone();
    }

    @NotNull
    public Map<ClickType, MenuAction> handlers() {
        return this.handlers;
    }

    public boolean closeAfterClick() {
        return this.closeAfterClick;
    }

    public boolean refreshAfterClick() {
        return this.refreshAfterClick;
    }

    public boolean handle(@NotNull MenuClickContext context) {
        requireNonNull(context, "context");
        MenuAction handler = this.handlers.get(context.clickType());
        if (handler == null) {
            handler = this.fallbackHandler;
        }
        if (handler == null) {
            return false;
        }

        handler.click(context);
        return true;
    }

    public static final class Builder {
        private final ItemStack itemStack;
        private final Map<ClickType, MenuAction> handlers = new EnumMap<>(ClickType.class);
        private MenuAction fallbackHandler;
        private boolean closeAfterClick;
        private boolean refreshAfterClick;

        private Builder(@NotNull ItemStack itemStack) {
            this.itemStack = requireNonNull(itemStack, "itemStack").clone();
        }

        @NotNull
        public Builder on(@NotNull ClickType type, @Nullable MenuAction handler) {
            requireNonNull(type, "type");
            if (handler == null) {
                this.handlers.remove(type);
            } else {
                this.handlers.put(type, handler);
            }
            return this;
        }

        @NotNull
        public Builder on(@NotNull ClickType type, @Nullable Runnable handler) {
            return on(type, handler == null ? null : context -> handler.run());
        }

        @NotNull
        public Builder onAny(@Nullable MenuAction handler) {
            this.fallbackHandler = handler;
            return this;
        }

        @NotNull
        public Builder onAny(@Nullable Runnable handler) {
            this.fallbackHandler = handler == null ? null : context -> handler.run();
            return this;
        }

        @NotNull
        public Builder closeAfterClick(boolean closeAfterClick) {
            this.closeAfterClick = closeAfterClick;
            return this;
        }

        @NotNull
        public Builder refreshAfterClick(boolean refreshAfterClick) {
            this.refreshAfterClick = refreshAfterClick;
            return this;
        }

        @NotNull
        public MenuButton build() {
            return new MenuButton(this.itemStack, this.handlers, this.fallbackHandler, this.closeAfterClick, this.refreshAfterClick);
        }
    }
}
