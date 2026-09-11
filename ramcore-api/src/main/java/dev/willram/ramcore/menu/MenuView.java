package dev.willram.ramcore.menu;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.Component;
import dev.willram.ramcore.text.TextContext;
import dev.willram.ramcore.text.Texts;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Declarative inventory menu definition.
 */
public final class MenuView {
    private final Component title;
    private final int rows;
    private final Map<Integer, MenuButton> buttons;
    private final MenuRenderer renderer;
    private final MenuLifecycleHandler openHandler;
    private final MenuLifecycleHandler closeHandler;
    private final MenuLifecycleHandler tickHandler;
    private final boolean playerInventoryClicksAllowed;
    private final boolean dragAllowed;
    private final long updateIntervalTicks;

    private MenuView(
            @NotNull Component title,
            int rows,
            @NotNull Map<Integer, MenuButton> buttons,
            @NotNull MenuRenderer renderer,
            @NotNull MenuLifecycleHandler openHandler,
            @NotNull MenuLifecycleHandler closeHandler,
            @NotNull MenuLifecycleHandler tickHandler,
            boolean playerInventoryClicksAllowed,
            boolean dragAllowed,
            long updateIntervalTicks
    ) {
        this.title = requireNonNull(title, "title");
        this.rows = validateRows(rows);
        this.buttons = Map.copyOf(buttons);
        this.renderer = requireNonNull(renderer, "renderer");
        this.openHandler = requireNonNull(openHandler, "openHandler");
        this.closeHandler = requireNonNull(closeHandler, "closeHandler");
        this.tickHandler = requireNonNull(tickHandler, "tickHandler");
        this.playerInventoryClicksAllowed = playerInventoryClicksAllowed;
        this.dragAllowed = dragAllowed;
        this.updateIntervalTicks = updateIntervalTicks;
        RamPreconditions.checkArgument(updateIntervalTicks >= 0L, "menu update interval must be >= 0", "Use 0 to disable periodic updates.");
    }

    @NotNull
    public static Builder builder(@NotNull ComponentLike title, int rows) {
        return new Builder(title.asComponent(), rows);
    }

    @NotNull
    public static Builder builder(@NotNull String title, int rows) {
        return builder(MiniMessage.miniMessage().deserialize(title), rows);
    }

    @NotNull
    public static Builder builder(@NotNull String title, @NotNull TextContext context, int rows) {
        return builder(Texts.render(title, context), rows);
    }

    @NotNull
    public Component title() {
        return this.title;
    }

    public int rows() {
        return this.rows;
    }

    public int size() {
        return this.rows * 9;
    }

    @NotNull
    public Map<Integer, MenuButton> buttons() {
        return this.buttons;
    }

    @NotNull
    public MenuRenderer renderer() {
        return this.renderer;
    }

    @NotNull
    public MenuLifecycleHandler openHandler() {
        return this.openHandler;
    }

    @NotNull
    public MenuLifecycleHandler closeHandler() {
        return this.closeHandler;
    }

    @NotNull
    public MenuLifecycleHandler tickHandler() {
        return this.tickHandler;
    }

    public boolean playerInventoryClicksAllowed() {
        return this.playerInventoryClicksAllowed;
    }

    public boolean dragAllowed() {
        return this.dragAllowed;
    }

    public long updateIntervalTicks() {
        return this.updateIntervalTicks;
    }

    static int validateRows(int rows) {
        RamPreconditions.checkArgument(rows >= 1 && rows <= 6, "menu rows must be between 1 and 6", "Use a chest inventory size from 1 to 6 rows.");
        return rows;
    }

    public static final class Builder {
        private final Component title;
        private final int rows;
        private final Map<Integer, MenuButton> buttons = new LinkedHashMap<>();
        private MenuRenderer renderer = session -> {};
        private MenuLifecycleHandler openHandler = session -> {};
        private MenuLifecycleHandler closeHandler = session -> {};
        private MenuLifecycleHandler tickHandler = session -> {};
        private boolean playerInventoryClicksAllowed;
        private boolean dragAllowed;
        private long updateIntervalTicks;

        private Builder(Component title, int rows) {
            this.title = requireNonNull(title, "title");
            this.rows = validateRows(rows);
        }

        @NotNull
        public Builder button(int slot, @NotNull MenuButton button) {
            validateSlot(slot, this.rows * 9);
            this.buttons.put(slot, requireNonNull(button, "button"));
            return this;
        }

        @NotNull
        public Builder item(int slot, @NotNull Item item) {
            return button(slot, MenuButton.fromItem(item));
        }

        @NotNull
        public Builder render(@NotNull MenuRenderer renderer) {
            this.renderer = requireNonNull(renderer, "renderer");
            return this;
        }

        @NotNull
        public Builder onOpen(@NotNull MenuLifecycleHandler openHandler) {
            this.openHandler = requireNonNull(openHandler, "openHandler");
            return this;
        }

        @NotNull
        public Builder onClose(@NotNull MenuLifecycleHandler closeHandler) {
            this.closeHandler = requireNonNull(closeHandler, "closeHandler");
            return this;
        }

        @NotNull
        public Builder onTick(@NotNull MenuLifecycleHandler tickHandler) {
            this.tickHandler = requireNonNull(tickHandler, "tickHandler");
            return this;
        }

        @NotNull
        public Builder allowPlayerInventoryClicks(boolean allowed) {
            this.playerInventoryClicksAllowed = allowed;
            return this;
        }

        @NotNull
        public Builder allowDrag(boolean allowed) {
            this.dragAllowed = allowed;
            return this;
        }

        @NotNull
        public Builder updateEveryTicks(long updateIntervalTicks) {
            this.updateIntervalTicks = updateIntervalTicks;
            return this;
        }

        @NotNull
        public MenuView build() {
            return new MenuView(
                    this.title,
                    this.rows,
                    this.buttons,
                    this.renderer,
                    this.openHandler,
                    this.closeHandler,
                    this.tickHandler,
                    this.playerInventoryClicksAllowed,
                    this.dragAllowed,
                    this.updateIntervalTicks
            );
        }

        static void validateSlot(int slot, int size) {
            RamPreconditions.checkArgument(slot >= 0 && slot < size, "menu slot is outside inventory bounds", "Use a slot from 0 to " + (size - 1) + ".");
        }
    }
}
