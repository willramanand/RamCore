package dev.willram.ramcore.npc;

import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Tracks managed NPC handles and dispatches Bukkit interactions to them.
 */
public final class NpcRegistry implements Listener, Terminable {
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private volatile boolean closed;

    private NpcRegistry() {
        this.plugin = null;
    }

    private NpcRegistry(@NotNull Plugin plugin) {
        this.plugin = requireNonNull(plugin, "plugin");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @NotNull
    public static NpcRegistry create() {
        return new NpcRegistry();
    }

    @NotNull
    public static NpcRegistry create(@NotNull Plugin plugin) {
        return new NpcRegistry(plugin);
    }

    @NotNull
    public Optional<Plugin> plugin() {
        return Optional.ofNullable(this.plugin);
    }

    public void register(@NotNull String owner, @NotNull NpcHandle<?> npc) {
        requireNonNull(owner, "owner");
        requireNonNull(npc, "npc");
        this.entries.put(npc.entity().getUniqueId(), new Entry(owner, npc));
    }

    @NotNull
    public Optional<NpcHandle<?>> npc(@NotNull Entity entity) {
        requireNonNull(entity, "entity");
        Entry entry = this.entries.get(entity.getUniqueId());
        return entry == null ? Optional.empty() : Optional.of(entry.npc());
    }

    @NotNull
    public Collection<NpcHandle<?>> all() {
        return this.entries.values().stream().map(Entry::npc).toList();
    }

    public boolean unregister(@NotNull Entity entity) {
        requireNonNull(entity, "entity");
        return this.entries.remove(entity.getUniqueId()) != null;
    }

    public void unregisterOwner(@NotNull String owner) {
        requireNonNull(owner, "owner");
        this.entries.entrySet().removeIf(entry -> {
            if (!entry.getValue().owner().equals(owner)) {
                return false;
            }
            entry.getValue().npc().close();
            return true;
        });
    }

    public boolean dispatch(@NotNull Player player, @NotNull Entity entity, @NotNull NpcClickType type) {
        requireNonNull(player, "player");
        requireNonNull(entity, "entity");
        requireNonNull(type, "type");
        Entry entry = this.entries.get(entity.getUniqueId());
        if (entry == null) {
            return false;
        }
        entry.npc().click(player, type);
        return entry.npc().spec().consumeClicks();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEntityEvent event) {
        if (dispatch(event.getPlayer(), event.getRightClicked(), NpcClickType.INTERACT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (dispatch(player, event.getEntity(), NpcClickType.ATTACK)) {
            event.setCancelled(true);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.plugin != null) {
            HandlerList.unregisterAll(this);
        }
        this.entries.values().forEach(entry -> entry.npc().close());
        this.entries.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    private record Entry(@NotNull String owner, @NotNull NpcHandle<?> npc) {
    }
}
