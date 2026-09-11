package dev.willram.ramcore.display;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Region-safe display spawning helpers.
 */
public final class DisplaySpawner {

    @NotNull
    public static <T extends Display> Promise<DisplayHandle<T>> spawn(@NotNull Location location, @NotNull DisplaySpec<T> spec) {
        requireNonNull(location, "location");
        requireNonNull(spec, "spec");
        World world = requireNonNull(location.getWorld(), "location world");
        Location spawnLocation = location.clone();
        return Schedulers.call(spawnLocation, () -> {
            T display = world.spawn(spawnLocation, spec.type(), spec::apply);
            return new DisplayHandle<>(display);
        });
    }

    private DisplaySpawner() {
    }
}
