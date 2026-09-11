package dev.willram.ramcore.npc;

import dev.willram.ramcore.promise.Promise;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Entry points for RamCore NPC helpers.
 */
public final class Npcs {

    @NotNull
    public static <T extends Entity> NpcSpec<T> spec(@NotNull Class<T> type) {
        return NpcSpec.of(type);
    }

    @NotNull
    public static <T extends Entity> Promise<NpcHandle<T>> spawn(@NotNull Location location, @NotNull NpcSpec<T> spec) {
        return NpcSpawner.spawn(location, spec);
    }

    @NotNull
    public static NpcRegistry registry(@NotNull Plugin plugin) {
        return NpcRegistry.create(plugin);
    }

    private Npcs() {
    }
}
