package dev.willram.ramcore.scheduler;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.exception.RamExceptions;
import dev.willram.ramcore.utils.LoaderUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class PaperFoliaSchedulerBackend implements SchedulerBackend {

    private static final int NO_BUKKIT_TASK_ID = -1;

    @Override
    public boolean isSyncThread() {
        return Bukkit.isPrimaryThread() || Bukkit.isGlobalTickThread();
    }

    @Override
    public void executeSync(@NotNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        if (isSyncThread()) {
            task.run();
            return;
        }

        Bukkit.getGlobalRegionScheduler().execute(plugin(), task);
    }

    @Override
    public void executeAsync(@NotNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        Bukkit.getAsyncScheduler().runNow(plugin(), scheduledTask -> task.run());
    }

    @Override
    public TaskHandle runDelayedSync(@NotNull Runnable runnable, long delayTicks) {
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        if (delayTicks <= 0) {
            executeSync(task);
            return new CompletedTaskHandle();
        }

        return new PaperTaskHandle(Bukkit.getGlobalRegionScheduler().runDelayed(plugin(), scheduledTask -> task.run(), delayTicks));
    }

    @Override
    public TaskHandle runDelayedAsync(@NotNull Runnable runnable, long delayTicks) {
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        if (delayTicks <= 0) {
            executeAsync(task);
            return new CompletedTaskHandle();
        }

        return new PaperTaskHandle(Bukkit.getGlobalRegionScheduler().runDelayed(plugin(), scheduledTask ->
                Bukkit.getAsyncScheduler().runNow(plugin(), asyncTask -> task.run()), delayTicks));
    }

    @Override
    public TaskHandle runDelayedAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit) {
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(unit, "unit");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        if (delay <= 0) {
            executeAsync(task);
            return new CompletedTaskHandle();
        }

        return new PaperTaskHandle(Bukkit.getAsyncScheduler().runDelayed(plugin(), scheduledTask -> task.run(), delay, unit));
    }

    @Override
    public TaskHandle runRepeatingSync(@NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        Objects.requireNonNull(runnable, "runnable");
        return new PaperTaskHandle(Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin(),
                scheduledTask -> runnable.run(),
                normalizeInitialDelay(delayTicks),
                normalizePeriod(intervalTicks)
        ));
    }

    @Override
    public TaskHandle runRepeatingAsync(@NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        Objects.requireNonNull(runnable, "runnable");
        return new PaperTaskHandle(Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin(),
                scheduledTask -> Bukkit.getAsyncScheduler().runNow(plugin(), asyncTask -> runnable.run()),
                normalizeInitialDelay(delayTicks),
                normalizePeriod(intervalTicks)
        ));
    }

    @Override
    public TaskHandle runRepeatingAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit delayUnit, long interval, @NotNull TimeUnit intervalUnit) {
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(delayUnit, "delayUnit");
        Objects.requireNonNull(intervalUnit, "intervalUnit");
        return new PaperTaskHandle(Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin(),
                scheduledTask -> runnable.run(),
                Math.max(0L, delayUnit.toNanos(delay)),
                Math.max(1L, intervalUnit.toNanos(interval)),
                TimeUnit.NANOSECONDS
        ));
    }

    @Override
    public void executeEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(retired, "retired");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        Runnable retiredTask = RamExceptions.wrapSchedulerTask(retired);
        if (!entity.getScheduler().execute(plugin(), task, retiredTask, 1L)) {
            retiredTask.run();
        }
    }

    @Override
    public TaskHandle runDelayedEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(retired, "retired");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        Runnable retiredTask = RamExceptions.wrapSchedulerTask(retired);
        ScheduledTask scheduledTask = entity.getScheduler().runDelayed(
                plugin(),
                ignored -> task.run(),
                retiredTask,
                normalizeInitialDelay(delayTicks)
        );
        if (scheduledTask == null) {
            retiredTask.run();
            return RetiredTaskHandle.INSTANCE;
        }
        return new PaperTaskHandle(scheduledTask);
    }

    @Override
    public TaskHandle runRepeatingEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks, long intervalTicks) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(retired, "retired");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        Runnable retiredTask = RamExceptions.wrapSchedulerTask(retired);
        ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(
                plugin(),
                ignored -> task.run(),
                retiredTask,
                normalizeInitialDelay(delayTicks),
                normalizePeriod(intervalTicks)
        );
        if (scheduledTask == null) {
            retiredTask.run();
            return RetiredTaskHandle.INSTANCE;
        }
        return new PaperTaskHandle(scheduledTask);
    }

    @Override
    public void executeRegion(@NotNull Location location, @NotNull Runnable runnable) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        Bukkit.getRegionScheduler().execute(plugin(), location, task);
    }

    @Override
    public TaskHandle runDelayedRegion(@NotNull Location location, @NotNull Runnable runnable, long delayTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        return new PaperTaskHandle(Bukkit.getRegionScheduler().runDelayed(
                plugin(),
                location,
                ignored -> task.run(),
                normalizeInitialDelay(delayTicks)
        ));
    }

    @Override
    public TaskHandle runRepeatingRegion(@NotNull Location location, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        return new PaperTaskHandle(Bukkit.getRegionScheduler().runAtFixedRate(
                plugin(),
                location,
                ignored -> task.run(),
                normalizeInitialDelay(delayTicks),
                normalizePeriod(intervalTicks)
        ));
    }

    @Override
    public void executeRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        Bukkit.getRegionScheduler().execute(plugin(), world, chunkX, chunkZ, task);
    }

    @Override
    public TaskHandle runDelayedRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        return new PaperTaskHandle(Bukkit.getRegionScheduler().runDelayed(
                plugin(),
                world,
                chunkX,
                chunkZ,
                ignored -> task.run(),
                normalizeInitialDelay(delayTicks)
        ));
    }

    @Override
    public TaskHandle runRepeatingRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(runnable, "runnable");
        Runnable task = RamExceptions.wrapSchedulerTask(runnable);
        return new PaperTaskHandle(Bukkit.getRegionScheduler().runAtFixedRate(
                plugin(),
                world,
                chunkX,
                chunkZ,
                ignored -> task.run(),
                normalizeInitialDelay(delayTicks),
                normalizePeriod(intervalTicks)
        ));
    }

    @Override
    public void cancelTasks(@NotNull RamPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }

    private static RamPlugin plugin() {
        return LoaderUtils.getPlugin();
    }

    private static long normalizeInitialDelay(long delayTicks) {
        return Math.max(1L, delayTicks);
    }

    private static long normalizePeriod(long intervalTicks) {
        return Math.max(1L, intervalTicks);
    }

    private static final class PaperTaskHandle implements TaskHandle {
        private final ScheduledTask task;

        private PaperTaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public boolean cancel() {
            ScheduledTask.CancelledState state = this.task.cancel();
            return state == ScheduledTask.CancelledState.CANCELLED_BY_CALLER
                    || state == ScheduledTask.CancelledState.NEXT_RUNS_CANCELLED;
        }

        @Override
        public boolean isCancelled() {
            return this.task.isCancelled();
        }

        @Override
        public int getBukkitId() {
            return NO_BUKKIT_TASK_ID;
        }
    }

    private static final class CompletedTaskHandle implements TaskHandle {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public boolean cancel() {
            return this.cancelled.compareAndSet(false, true);
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled.get();
        }

        @Override
        public int getBukkitId() {
            return NO_BUKKIT_TASK_ID;
        }
    }

    private enum RetiredTaskHandle implements TaskHandle {
        INSTANCE;

        @Override
        public boolean cancel() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public int getBukkitId() {
            return NO_BUKKIT_TASK_ID;
        }
    }
}
