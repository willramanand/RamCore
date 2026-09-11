package dev.willram.ramcore.testkit;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.exception.RamExceptions;
import dev.willram.ramcore.scheduler.SchedulerBackend;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.scheduler.Ticks;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deterministic, tick-stepped {@link SchedulerBackend} for unit tests.
 *
 * <p>Nothing runs until the test drives it: {@link #tick()} advances one server tick and runs due
 * global/region/entity work, {@link #runAsync()} drains the async queue inline, and
 * {@link #runAll()} loops both until idle. The thread that created the scheduler is the "sync"
 * thread; while {@link #runAsync()} is draining, {@link #isSyncThread()} answers {@code false} so
 * sync continuations queue exactly as they would on a server.</p>
 *
 * <p>Semantics mirror the Paper/Folia backend: immediate sync work runs inline when already on the
 * sync thread, delayed and repeating work is normalised to at least one tick, and entity work for a
 * retired entity runs its retired callback instead.</p>
 */
public final class FakeScheduler implements SchedulerBackend, AutoCloseable {
    private final Thread mainThread = Thread.currentThread();
    private final Deque<Pending> syncQueue = new ArrayDeque<>();
    private final Deque<Pending> asyncQueue = new ArrayDeque<>();
    private final List<ScheduledEntry> scheduled = new ArrayList<>();
    private final Set<UUID> retiredEntities = new HashSet<>();
    private final List<String> executed = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();
    private long tick;
    private long sequence;
    private boolean inAsync;
    private boolean installed;

    /**
     * Creates a scheduler and installs it as the active backend.
     *
     * @return the installed scheduler; call {@link #close()} to restore the default backend
     */
    @NotNull
    public static FakeScheduler install() {
        FakeScheduler scheduler = new FakeScheduler();
        SchedulerBackends.install(scheduler);
        scheduler.installed = true;
        return scheduler;
    }

    @Override
    public void close() {
        if (this.installed) {
            SchedulerBackends.reset();
            this.installed = false;
        }
    }

    // ---- observation ----

    public long currentTick() {
        return this.tick;
    }

    public int pendingSync() {
        return this.syncQueue.size();
    }

    public int pendingAsync() {
        return this.asyncQueue.size();
    }

    public int pendingScheduled() {
        return (int) this.scheduled.stream().filter(entry -> !entry.cancelled.get()).count();
    }

    /**
     * Context descriptions of every runnable executed so far, in order:
     * {@code global}, {@code async}, {@code entity:<uuid>}, {@code region:<world>@x,y,z},
     * {@code chunk:<world>@x,z}.
     */
    @NotNull
    public List<String> executed() {
        return List.copyOf(this.executed);
    }

    /**
     * Throwables thrown by executed runnables. They are also reported through
     * {@link RamExceptions} like the server backend does.
     */
    @NotNull
    public List<Throwable> errors() {
        return List.copyOf(this.errors);
    }

    // ---- driving ----

    /** Advances one tick: runs due delayed/repeating work, then drains immediate sync work. */
    public void tick() {
        tick(1L);
    }

    public void tick(long ticks) {
        for (long i = 0; i < ticks; i++) {
            this.tick++;
            runDue();
            drainSync();
        }
    }

    /**
     * Drains the async queue inline. {@link #isSyncThread()} answers {@code false} while draining.
     *
     * @return number of runnables executed
     */
    public int runAsync() {
        int count = 0;
        this.inAsync = true;
        try {
            Pending pending;
            while ((pending = this.asyncQueue.poll()) != null) {
                run(pending);
                count++;
            }
        } finally {
            this.inAsync = false;
        }
        return count;
    }

    /** Drains sync and async queues repeatedly until both are empty. Does not advance ticks. */
    public void runAll() {
        int guard = 0;
        while (!this.syncQueue.isEmpty() || !this.asyncQueue.isEmpty()) {
            drainSync();
            runAsync();
            if (++guard > 10_000) {
                throw new IllegalStateException("FakeScheduler.runAll did not settle after 10000 rounds");
            }
        }
    }

    /**
     * Marks an entity as removed. Queued and scheduled work for it runs its retired callback and is
     * dropped; later submissions for that entity retire immediately.
     */
    public void retireEntity(@NotNull UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        this.retiredEntities.add(entityId);
        retireQueued(this.syncQueue, entityId);
        retireQueued(this.asyncQueue, entityId);
        Iterator<ScheduledEntry> iterator = this.scheduled.iterator();
        while (iterator.hasNext()) {
            ScheduledEntry entry = iterator.next();
            if (entityId.equals(entry.entityId)) {
                iterator.remove();
                entry.cancelled.set(true);
                runRetired(entry.retired);
            }
        }
    }

    // ---- SchedulerBackend ----

    @Override
    public boolean isSyncThread() {
        return isSyncThread(Thread.currentThread());
    }

    @Override
    public boolean isSyncThread(@NotNull Thread thread) {
        return thread == this.mainThread && !this.inAsync;
    }

    @Override
    public void executeSync(@NotNull Runnable runnable) {
        Pending pending = new Pending("global", runnable, null, null);
        if (isSyncThread()) {
            run(pending);
            return;
        }
        this.syncQueue.add(pending);
    }

    @Override
    public void executeAsync(@NotNull Runnable runnable) {
        this.asyncQueue.add(new Pending("async", runnable, null, null));
    }

    @Override
    public TaskHandle runDelayedSync(@NotNull Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            executeSync(runnable);
            return new CompletedHandle();
        }
        return schedule("global", runnable, delayTicks, -1L, false, null, null);
    }

    @Override
    public TaskHandle runDelayedAsync(@NotNull Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            executeAsync(runnable);
            return new CompletedHandle();
        }
        return schedule("async", runnable, delayTicks, -1L, true, null, null);
    }

    @Override
    public TaskHandle runDelayedAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit) {
        return runDelayedAsync(runnable, Ticks.from(delay, unit));
    }

    @Override
    public TaskHandle runRepeatingSync(@NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return schedule("global", runnable, atLeastOne(delayTicks), atLeastOne(intervalTicks), false, null, null);
    }

    @Override
    public TaskHandle runRepeatingAsync(@NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return schedule("async", runnable, atLeastOne(delayTicks), atLeastOne(intervalTicks), true, null, null);
    }

    @Override
    public TaskHandle runRepeatingAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit delayUnit, long interval, @NotNull TimeUnit intervalUnit) {
        return runRepeatingAsync(runnable, Ticks.from(delay, delayUnit), Ticks.from(interval, intervalUnit));
    }

    @Override
    public void executeEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired) {
        UUID id = entity.getUniqueId();
        if (this.retiredEntities.contains(id)) {
            runRetired(retired);
            return;
        }
        this.syncQueue.add(new Pending(entityContext(id), runnable, id, retired));
    }

    @Override
    public TaskHandle runDelayedEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks) {
        UUID id = entity.getUniqueId();
        if (this.retiredEntities.contains(id)) {
            runRetired(retired);
            return RetiredHandle.INSTANCE;
        }
        return schedule(entityContext(id), runnable, atLeastOne(delayTicks), -1L, false, id, retired);
    }

    @Override
    public TaskHandle runRepeatingEntity(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks, long intervalTicks) {
        UUID id = entity.getUniqueId();
        if (this.retiredEntities.contains(id)) {
            runRetired(retired);
            return RetiredHandle.INSTANCE;
        }
        return schedule(entityContext(id), runnable, atLeastOne(delayTicks), atLeastOne(intervalTicks), false, id, retired);
    }

    @Override
    public void executeRegion(@NotNull Location location, @NotNull Runnable runnable) {
        this.syncQueue.add(new Pending(regionContext(location), runnable, null, null));
    }

    @Override
    public TaskHandle runDelayedRegion(@NotNull Location location, @NotNull Runnable runnable, long delayTicks) {
        return schedule(regionContext(location), runnable, atLeastOne(delayTicks), -1L, false, null, null);
    }

    @Override
    public TaskHandle runRepeatingRegion(@NotNull Location location, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return schedule(regionContext(location), runnable, atLeastOne(delayTicks), atLeastOne(intervalTicks), false, null, null);
    }

    @Override
    public void executeRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable) {
        this.syncQueue.add(new Pending(chunkContext(world, chunkX, chunkZ), runnable, null, null));
    }

    @Override
    public TaskHandle runDelayedRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks) {
        return schedule(chunkContext(world, chunkX, chunkZ), runnable, atLeastOne(delayTicks), -1L, false, null, null);
    }

    @Override
    public TaskHandle runRepeatingRegion(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return schedule(chunkContext(world, chunkX, chunkZ), runnable, atLeastOne(delayTicks), atLeastOne(intervalTicks), false, null, null);
    }

    @Override
    public void cancelTasks(@NotNull RamPlugin plugin) {
        this.syncQueue.clear();
        this.asyncQueue.clear();
        this.scheduled.forEach(entry -> entry.cancelled.set(true));
        this.scheduled.clear();
    }

    // ---- internals ----

    private ScheduledEntry schedule(String context, Runnable runnable, long dueIn, long interval, boolean async, @Nullable UUID entityId, @Nullable Runnable retired) {
        ScheduledEntry entry = new ScheduledEntry(context, runnable, this.tick + dueIn, interval, async, entityId, retired, this.sequence++);
        this.scheduled.add(entry);
        return entry;
    }

    private void runDue() {
        List<ScheduledEntry> due = new ArrayList<>();
        for (ScheduledEntry entry : this.scheduled) {
            if (!entry.cancelled.get() && entry.dueTick <= this.tick) {
                due.add(entry);
            }
        }
        due.sort(Comparator.comparingLong((ScheduledEntry e) -> e.dueTick).thenComparingLong(e -> e.sequence));
        for (ScheduledEntry entry : due) {
            if (entry.cancelled.get()) {
                continue;
            }
            if (entry.interval > 0) {
                entry.dueTick += entry.interval;
            } else {
                entry.cancelled.set(true);
                this.scheduled.remove(entry);
            }
            Pending pending = new Pending(entry.context, entry.runnable, entry.entityId, entry.retired);
            if (entry.async) {
                this.asyncQueue.add(pending);
            } else {
                run(pending);
            }
        }
        this.scheduled.removeIf(entry -> entry.cancelled.get());
    }

    private void drainSync() {
        Pending pending;
        while ((pending = this.syncQueue.poll()) != null) {
            run(pending);
        }
    }

    private void run(Pending pending) {
        if (pending.entityId != null && this.retiredEntities.contains(pending.entityId)) {
            runRetired(pending.retired);
            return;
        }
        this.executed.add(pending.context);
        try {
            pending.runnable.run();
        } catch (Throwable t) {
            this.errors.add(t);
            RamExceptions.reportScheduler(t);
        }
    }

    private void runRetired(@Nullable Runnable retired) {
        if (retired == null) {
            return;
        }
        try {
            retired.run();
        } catch (Throwable t) {
            this.errors.add(t);
            RamExceptions.reportScheduler(t);
        }
    }

    private void retireQueued(Deque<Pending> queue, UUID entityId) {
        Iterator<Pending> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (entityId.equals(pending.entityId)) {
                iterator.remove();
                runRetired(pending.retired);
            }
        }
    }

    private static long atLeastOne(long ticks) {
        return Math.max(1L, ticks);
    }

    private static String entityContext(UUID id) {
        return "entity:" + id;
    }

    private static String regionContext(Location location) {
        World world = location.getWorld();
        return "region:" + (world == null ? "?" : world.getName()) + "@" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private static String chunkContext(World world, int chunkX, int chunkZ) {
        return "chunk:" + world.getName() + "@" + chunkX + "," + chunkZ;
    }

    private record Pending(String context, Runnable runnable, @Nullable UUID entityId, @Nullable Runnable retired) {
    }

    private static final class ScheduledEntry implements TaskHandle {
        private final String context;
        private final Runnable runnable;
        private final long interval;
        private final boolean async;
        private final @Nullable UUID entityId;
        private final @Nullable Runnable retired;
        private final long sequence;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private long dueTick;

        private ScheduledEntry(String context, Runnable runnable, long dueTick, long interval, boolean async, @Nullable UUID entityId, @Nullable Runnable retired, long sequence) {
            this.context = context;
            this.runnable = runnable;
            this.dueTick = dueTick;
            this.interval = interval;
            this.async = async;
            this.entityId = entityId;
            this.retired = retired;
            this.sequence = sequence;
        }

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
            return -1;
        }
    }

    private static final class CompletedHandle implements TaskHandle {
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
            return -1;
        }
    }

    private enum RetiredHandle implements TaskHandle {
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
            return -1;
        }
    }
}
