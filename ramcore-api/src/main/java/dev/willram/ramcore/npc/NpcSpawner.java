package dev.willram.ramcore.npc;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Region-safe NPC spawning helpers.
 */
public final class NpcSpawner {

    @NotNull
    public static <T extends Entity> Promise<NpcHandle<T>> spawn(@NotNull Location location, @NotNull NpcSpec<T> spec) {
        requireNonNull(location, "location");
        requireNonNull(spec, "spec");
        World world = requireNonNull(location.getWorld(), "location world");
        Location spawnLocation = location.clone();
        return Schedulers.call(spawnLocation, () -> {
            T entity = world.spawn(spawnLocation, spec.type(), spec::apply);
            return new NpcHandle<>(entity, spec);
        });
    }

    private NpcSpawner() {
    }
}
