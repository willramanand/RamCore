package dev.willram.ramcore.playerdata;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Bridges Bukkit's login lifecycle to a {@link PlayerDataService}. Registered automatically by
 * {@link PlayerDataService#install}; construct one yourself only for a service created with
 * {@link PlayerDataService#create}.
 *
 * <p>Priorities: pre-login and login at {@code MONITOR} so a disallow by another plugin is seen;
 * join at {@code LOWEST} so the data is promoted before other join handlers run; quit at
 * {@code MONITOR} so other quit handlers can still mutate the data before it is saved.</p>
 */
public final class PlayerDataListener implements Listener {
    private final PlayerDataService service;

    public PlayerDataListener(@NotNull PlayerDataService service) {
        this.service = requireNonNull(service, "service");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            this.service.preload(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(@NotNull PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            this.service.cancelPending(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        this.service.join(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        this.service.quit(event.getPlayer().getUniqueId());
    }
}
