package dev.willram.ramcore.scheduler;

import dev.willram.ramcore.RamPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;

/**
 * Dispatch surface behind {@link Schedulers}. Internal: implemented by the Paper/Folia backend and by
 * the test kit's deterministic scheduler.
 */
@ApiStatus.Internal
public interface SchedulerBackend {

    boolean isSyncThread();

    boolean isSyncThread(@NotNull Thread thread);

    void executeSync(@NotNull Runnable runnable);

    void executeAsync(@NotNull Runnable runnable);

    TaskHandle runDelayedSync(@NotNull Runnable runnable, long delayTicks);

    TaskHandle runDelayedAsync(@NotNull Runnable runnable, long delayTicks);

    TaskHandle runDelayedAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit);

    TaskHandle runRepeatingSync(@NotNull Runnable runnable, long delayTicks, long intervalTicks);

    TaskHandle runRepeatingAsync(@NotNull Runnable runnable, long delayTicks, long intervalTicks);

    TaskHandle runRepeatingAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit delayUnit, long interval, @NotNull TimeUnit intervalUnit);

    void executeEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired);

    TaskHandle runDelayedEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks);

    TaskHandle runRepeatingEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks, long intervalTicks);

    void executeRegion(@NotNull Location location, @NotNull Runnable runnable);

    TaskHandle runDelayedRegion(@NotNull Location location, @NotNull Runnable runnable, long delayTicks);

    TaskHandle runRepeatingRegion(@NotNull Location location, @NotNull Runnable runnable, long delayTicks, long intervalTicks);

    void executeRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable);

    TaskHandle runDelayedRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks);

    TaskHandle runRepeatingRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks, long intervalTicks);

    void cancelTasks(@NotNull RamPlugin plugin);

    interface TaskHandle {
        boolean cancel();

        boolean isCancelled();

        int getBukkitId();
    }
}
