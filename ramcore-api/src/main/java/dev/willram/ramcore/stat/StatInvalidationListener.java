package dev.willram.ramcore.stat;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Invalidates a player's cached {@link StatSnapshot} when their equipment or held item changes, and
 * evicts it on quit. Registered by {@link StatService#install(dev.willram.ramcore.RamPlugin, StatRegistry)}.
 */
final class StatInvalidationListener implements Listener {
    private final StatService service;

    StatInvalidationListener(@NotNull StatService service) {
        this.service = requireNonNull(service, "service");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(@NotNull PlayerArmorChangeEvent event) {
        this.service.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        this.service.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        this.service.invalidate(event.getPlayer().getUniqueId());
    }
}
