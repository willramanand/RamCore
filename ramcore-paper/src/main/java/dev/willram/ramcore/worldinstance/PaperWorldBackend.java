package dev.willram.ramcore.worldinstance;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.TaskContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * The Paper {@link WorldBackend}: loads worlds with {@code Bukkit.createWorld} on the global region,
 * unloads with {@code Bukkit.unloadWorld}, and evacuates players with {@code teleportAsync}. Runtime
 * world creation is unsupported on Folia (regionised), where {@link #supportsInstances()} returns
 * false so the service refuses with an actionable message. Paper-experimental.
 */
public final class PaperWorldBackend implements WorldBackend {
    private static final boolean FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");

    @Override
    public boolean supportsInstances() {
        return !FOLIA;
    }

    @Override
    @NotNull
    public Path worldContainer() {
        return Bukkit.getWorldContainer().toPath();
    }

    @Override
    @NotNull
    public Promise<Void> loadWorld(@NotNull String name) {
        return Schedulers.run(TaskContext.global(), () -> Bukkit.createWorld(new WorldCreator(name)));
    }

    @Override
    @NotNull
    public Promise<Void> unloadWorld(@NotNull String name, boolean save) {
        return Schedulers.run(TaskContext.global(), () -> {
            World world = Bukkit.getWorld(name);
            if (world != null) {
                Bukkit.unloadWorld(world, save);
            }
        });
    }

    @Override
    @NotNull
    public Promise<Void> evacuate(@NotNull String name) {
        return Schedulers.run(TaskContext.global(), () -> {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                return;
            }
            Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
            for (Player player : List.copyOf(world.getPlayers())) {
                player.teleportAsync(fallback);
            }
        });
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }
}
