package dev.willram.ramcore.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Context passed to declarative menu button handlers.
 */
public record MenuClickContext(
        @NotNull MenuSession session,
        @NotNull InventoryClickEvent event,
        int slot,
        @NotNull MenuButton button
) {
    public MenuClickContext {
        requireNonNull(session, "session");
        requireNonNull(event, "event");
        requireNonNull(button, "button");
    }

    @NotNull
    public ClickType clickType() {
        return this.event.getClick();
    }
}
