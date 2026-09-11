package dev.willram.ramcore.input;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Forwards Bukkit events to an {@link InputSessionRegistry}. Registered by
 * {@link PlayerInput#install}. Chat is handled at {@link EventPriority#LOWEST} so the message is
 * captured and cancelled before other chat listeners run.
 */
public final class InputListener implements Listener {
    private final InputSessionRegistry registry;

    public InputListener(@NotNull InputSessionRegistry registry) {
        this.registry = requireNonNull(registry, "registry");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {
        this.registry.onChat(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player
                && this.registry.onAnvilClick(player, event.getView(), event.getRawSlot())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (event.getPlayer() instanceof org.bukkit.entity.Player player) {
            this.registry.onInventoryClose(player, event.getView());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        this.registry.onQuit(event.getPlayer().getUniqueId());
    }
}
