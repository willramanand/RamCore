/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.willram.ramcore.scheduler;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.exception.RamExceptions;
import dev.willram.ramcore.interfaces.Delegate;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.promise.ThreadContext;
import dev.willram.ramcore.scheduler.builder.TaskBuilder;
import dev.willram.ramcore.terminable.TerminableConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides common instances of {@link Scheduler}.
 */
public final class Schedulers {
    private static final SchedulerBackend BACKEND = new PaperFoliaSchedulerBackend();
    private static final Scheduler SYNC_SCHEDULER = new SyncScheduler();
    private static final Scheduler ASYNC_SCHEDULER = new AsyncScheduler();

    /**
     * Gets a scheduler for the given context.
     *
     * @param context the context
     * @return a scheduler
     */
    public static Scheduler get(ThreadContext context) {
        return switch (context) {
            case SYNC -> sync();
            case ASYNC -> async();
            default -> throw new AssertionError();
        };
    }

    /**
     * Returns a "sync" scheduler, which executes tasks on the main server thread.
     *
     * @return a sync executor instance
     */
    public static Scheduler sync() {
        return SYNC_SCHEDULER;
    }

    /**
     * Returns an "async" scheduler, which executes tasks asynchronously.
     *
     * @return an async executor instance
     */
    public static Scheduler async() {
        return ASYNC_SCHEDULER;
    }

    /**
     * Returns a scheduler for work that must run on the region owning an entity.
     *
     * <p>On Folia this uses the entity scheduler, so the task follows the entity if it
     * changes region. On Paper this is handled by Paper's compatibility scheduler.</p>
     *
     * @param entity the entity that owns the scheduled work
     * @return an entity-bound scheduler
     */
    public static Scheduler forEntity(@NotNull Entity entity) {
        return new EntityBoundScheduler(entity);
    }

    /**
     * Returns a scheduler for work that must run on the region owning a player.
     *
     * @param player the player that owns the scheduled work
     * @return an entity-bound scheduler
     */
    public static Scheduler forPlayer(@NotNull Player player) {
        return forEntity(player);
    }

    /**
     * Returns a scheduler for work that must run on the region owning a location.
     *
     * @param location the location whose region owns the scheduled work
     * @return a region-bound scheduler
     */
    public static Scheduler forRegion(@NotNull Location location) {
        return new RegionBoundScheduler(location);
    }

    /**
     * Returns a scheduler for work that must run on the region owning a block.
     *
     * @param block the block whose region owns the scheduled work
     * @return a region-bound scheduler
     */
    public static Scheduler forBlock(@NotNull Block block) {
        return forRegion(Objects.requireNonNull(block, "block").getLocation());
    }

    /**
     * Returns a scheduler for work that must run on the region owning a block state.
     *
     * @param blockState the block state whose region owns the scheduled work
     * @return a region-bound scheduler
     */
    public static Scheduler forBlockState(@NotNull BlockState blockState) {
        return forRegion(Objects.requireNonNull(blockState, "blockState").getLocation());
    }

    /**
     * Returns a scheduler for work that must run on the region owning a chunk.
     *
     * @param world the world containing the chunk
     * @param chunkX the chunk x coordinate
     * @param chunkZ the chunk z coordinate
     * @return a region-bound scheduler
     */
    public static Scheduler forChunk(@NotNull World world, int chunkX, int chunkZ) {
        return new ChunkBoundScheduler(world, chunkX, chunkZ);
    }

    /**
     * Returns a scheduler for work that must run on the region owning a chunk.
     *
     * @param chunk the chunk whose region owns the scheduled work
     * @return a region-bound scheduler
     */
    public static Scheduler forChunk(@NotNull Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return forChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    /**
     * Gets Bukkit's scheduler.
     *
     * @return bukkit's scheduler
     */
    public static BukkitScheduler bukkit() {
        return Bukkit.getServer().getScheduler();
    }

    /**
     * Gets a {@link TaskBuilder} instance
     *
     * @return a task builder
     */
    public static TaskBuilder builder() {
        return TaskBuilder.newBuilder();
    }

    @NotNull
    public static Promise<Void> run(@NotNull TaskContext context, @NotNull Runnable runnable) {
        return scheduler(context).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Entity entity, @NotNull Runnable runnable) {
        return forEntity(entity).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Location location, @NotNull Runnable runnable) {
        return forRegion(location).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Block block, @NotNull Runnable runnable) {
        return forBlock(block).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull BlockState blockState, @NotNull Runnable runnable) {
        return forBlockState(blockState).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Chunk chunk, @NotNull Runnable runnable) {
        return forChunk(chunk).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable) {
        return forChunk(world, chunkX, chunkZ).run(runnable);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(retired, "retired");
        Promise<Void> promise = Promise.empty();
        BACKEND.executeEntity(entity, () -> completePromise(promise, runnable), () -> {
            retired.run();
            promise.cancel();
        });
        return promise;
    }

    @NotNull
    public static Promise<Void> runGlobal(@NotNull Runnable runnable) {
        return sync().run(runnable);
    }

    @NotNull
    public static Promise<Void> runAsync(@NotNull Runnable runnable) {
        return async().run(runnable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull TaskContext context, @NotNull Callable<T> callable) {
        return scheduler(context).call(callable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull Entity entity, @NotNull Callable<T> callable) {
        return forEntity(entity).call(callable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull Location location, @NotNull Callable<T> callable) {
        return forRegion(location).call(callable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull Block block, @NotNull Callable<T> callable) {
        return forBlock(block).call(callable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull BlockState blockState, @NotNull Callable<T> callable) {
        return forBlockState(blockState).call(callable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull Chunk chunk, @NotNull Callable<T> callable) {
        return forChunk(chunk).call(callable);
    }

    @NotNull
    public static <T> Promise<T> call(@NotNull World world, int chunkX, int chunkZ, @NotNull Callable<T> callable) {
        return forChunk(world, chunkX, chunkZ).call(callable);
    }

    @NotNull
    public static <T> Promise<T> callGlobal(@NotNull Callable<T> callable) {
        return sync().call(callable);
    }

    @NotNull
    public static <T> Promise<T> callAsync(@NotNull Callable<T> callable) {
        return async().call(callable);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull TaskContext context, @NotNull Runnable runnable, long delayTicks) {
        return scheduler(context).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull TaskContext context, long delayTicks, @NotNull Runnable runnable) {
        return runLater(context, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Entity entity, @NotNull Runnable runnable, long delayTicks) {
        return forEntity(entity).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Entity entity, long delayTicks, @NotNull Runnable runnable) {
        return runLater(entity, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Location location, @NotNull Runnable runnable, long delayTicks) {
        return forRegion(location).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Location location, long delayTicks, @NotNull Runnable runnable) {
        return runLater(location, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Block block, @NotNull Runnable runnable, long delayTicks) {
        return forBlock(block).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Block block, long delayTicks, @NotNull Runnable runnable) {
        return runLater(block, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull BlockState blockState, @NotNull Runnable runnable, long delayTicks) {
        return forBlockState(blockState).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull BlockState blockState, long delayTicks, @NotNull Runnable runnable) {
        return runLater(blockState, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Chunk chunk, @NotNull Runnable runnable, long delayTicks) {
        return forChunk(chunk).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Chunk chunk, long delayTicks, @NotNull Runnable runnable) {
        return runLater(chunk, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks) {
        return forChunk(world, chunkX, chunkZ).runLater(runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull World world, int chunkX, int chunkZ, long delayTicks, @NotNull Runnable runnable) {
        return runLater(world, chunkX, chunkZ, runnable, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(retired, "retired");
        Promise<Void> promise = Promise.empty();
        BACKEND.runDelayedEntity(entity, () -> completePromise(promise, runnable), () -> {
            retired.run();
            promise.cancel();
        }, delayTicks);
        return promise;
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Entity entity, long delayTicks, @NotNull Runnable retired, @NotNull Runnable runnable) {
        return runLater(entity, runnable, retired, delayTicks);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull TaskContext context, @NotNull Runnable runnable, long delay, @NotNull TimeUnit unit) {
        return scheduler(context).runLater(runnable, delay, unit);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull TaskContext context, long delay, @NotNull TimeUnit unit, @NotNull Runnable runnable) {
        return runLater(context, runnable, delay, unit);
    }

    @NotNull
    public static Task runTimer(@NotNull TaskContext context, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return scheduler(context).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull TaskContext context, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(context, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return forEntity(entity).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Entity entity, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(entity, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Location location, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return forRegion(location).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Location location, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(location, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Block block, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return forBlock(block).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Block block, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(block, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull BlockState blockState, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return forBlockState(blockState).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull BlockState blockState, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(blockState, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Chunk chunk, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return forChunk(chunk).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Chunk chunk, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(chunk, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks, long intervalTicks) {
        return forChunk(world, chunkX, chunkZ).runRepeating(runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull World world, int chunkX, int chunkZ, long delayTicks, long intervalTicks, @NotNull Runnable runnable) {
        return runTimer(world, chunkX, chunkZ, runnable, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delayTicks, long intervalTicks) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(retired, "retired");
        RamTask task = new RamTask(ignored -> runnable.run());
        task.setHandle(BACKEND.runRepeatingEntity(entity, task, () -> {
            task.stop();
            retired.run();
        }, delayTicks, intervalTicks));
        return task;
    }

    @NotNull
    public static Task runTimer(@NotNull Entity entity, long delayTicks, long intervalTicks, @NotNull Runnable retired, @NotNull Runnable runnable) {
        return runTimer(entity, runnable, retired, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimer(@NotNull TaskContext context, @NotNull Consumer<Task> consumer, long delayTicks, long intervalTicks) {
        return scheduler(context).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull TaskContext context, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return runTimer(context, consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull Entity entity, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return forEntity(entity).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull Location location, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return forRegion(location).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull Block block, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return forBlock(block).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull BlockState blockState, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return forBlockState(blockState).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull Chunk chunk, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return forChunk(chunk).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Task runTimerTask(@NotNull World world, int chunkX, int chunkZ, long delayTicks, long intervalTicks, @NotNull Consumer<Task> consumer) {
        return forChunk(world, chunkX, chunkZ).runRepeating(consumer, delayTicks, intervalTicks);
    }

    @NotNull
    public static Promise<Void> run(@NotNull TaskContext context, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(context, runnable), owner);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Entity entity, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(entity, runnable), owner);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Location location, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(location, runnable), owner);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Block block, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(block, runnable), owner);
    }

    @NotNull
    public static Promise<Void> run(@NotNull BlockState blockState, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(blockState, runnable), owner);
    }

    @NotNull
    public static Promise<Void> run(@NotNull Chunk chunk, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(chunk, runnable), owner);
    }

    @NotNull
    public static Promise<Void> run(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, @NotNull TerminableConsumer owner) {
        return bind(run(world, chunkX, chunkZ, runnable), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull TaskContext context, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(context, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull TaskContext context, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(context, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(entity, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Entity entity, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(entity, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Location location, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(location, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Location location, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(location, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Block block, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(block, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Block block, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(block, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Task runTimer(@NotNull BlockState blockState, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(blockState, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull BlockState blockState, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(blockState, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Chunk chunk, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(chunk, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull Chunk chunk, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(chunk, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Task runTimer(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner) {
        return bind(runTimer(world, chunkX, chunkZ, runnable, delayTicks, intervalTicks), owner);
    }

    @NotNull
    public static Task runTimer(@NotNull World world, int chunkX, int chunkZ, long delayTicks, long intervalTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runTimer(world, chunkX, chunkZ, runnable, delayTicks, intervalTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull TaskContext context, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(context, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull TaskContext context, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(context, runnable, delayTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Entity entity, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(entity, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Entity entity, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(entity, runnable, delayTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Location location, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(location, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Location location, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(location, runnable, delayTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Block block, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(block, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Block block, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(block, runnable, delayTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull BlockState blockState, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(blockState, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull BlockState blockState, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(blockState, runnable, delayTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Chunk chunk, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(chunk, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull Chunk chunk, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(chunk, runnable, delayTicks, owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable runnable, long delayTicks, @NotNull TerminableConsumer owner) {
        return bind(runLater(world, chunkX, chunkZ, runnable, delayTicks), owner);
    }

    @NotNull
    public static Promise<Void> runLater(@NotNull World world, int chunkX, int chunkZ, long delayTicks, @NotNull TerminableConsumer owner, @NotNull Runnable runnable) {
        return runLater(world, chunkX, chunkZ, runnable, delayTicks, owner);
    }

    public static boolean isSyncThread() {
        return BACKEND.isSyncThread();
    }

    public static void shutdown(@NotNull RamPlugin plugin) {
        BACKEND.cancelTasks(plugin);
    }

    public static void executeDelayedSync(@NotNull Runnable runnable, long delayTicks) {
        BACKEND.runDelayedSync(runnable, delayTicks);
    }

    public static void executeDelayedAsync(@NotNull Runnable runnable, long delayTicks) {
        BACKEND.runDelayedAsync(runnable, delayTicks);
    }

    public static void executeDelayedAsync(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit) {
        BACKEND.runDelayedAsync(runnable, delay, unit);
    }

    @NotNull
    private static Scheduler scheduler(@NotNull TaskContext context) {
        Objects.requireNonNull(context, "context");
        return switch (context.type()) {
            case GLOBAL -> sync();
            case ASYNC -> async();
            case ENTITY -> forEntity(context.entity());
            case REGION -> forRegion(context.location());
            case CHUNK -> forChunk(context.world(), context.chunkX(), context.chunkZ());
        };
    }

    @NotNull
    private static <T extends AutoCloseable> T bind(@NotNull T terminable, @NotNull TerminableConsumer owner) {
        Objects.requireNonNull(owner, "owner");
        return owner.bind(terminable);
    }

    private static void completePromise(Promise<Void> promise, Runnable runnable) {
        if (promise.isCancelled()) {
            return;
        }

        try {
            runnable.run();
            promise.supply(null);
        } catch (Throwable t) {
            RamExceptions.reportScheduler(t);
            promise.supplyException(t);
        }
    }

    private static <T> void completePromise(Promise<T> promise, Callable<T> callable) {
        if (promise.isCancelled()) {
            return;
        }

        try {
            promise.supply(callable.call());
        } catch (Throwable t) {
            RamExceptions.reportPromise(t);
            promise.supplyException(t);
        }
    }

    private static final class SyncScheduler implements Scheduler {

        @Override
        public void execute(Runnable runnable) {
            BACKEND.executeSync(runnable);
        }

        @NotNull
        @Override
        public ThreadContext getContext() {
            return ThreadContext.SYNC;
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delayTicks, long intervalTicks) {
            Objects.requireNonNull(consumer, "consumer");
            RamTask task = new RamTask(consumer);
            task.setHandle(BACKEND.runRepeatingSync(task, delayTicks, intervalTicks));
            return task;
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delay, @NotNull TimeUnit delayUnit, long interval, @NotNull TimeUnit intervalUnit) {
            return runRepeating(consumer, Ticks.from(delay, delayUnit), Ticks.from(interval, intervalUnit));
        }
    }

    private static final class AsyncScheduler implements Scheduler {

        @Override
        public void execute(Runnable runnable) {
            BACKEND.executeAsync(runnable);
        }

        @NotNull
        @Override
        public ThreadContext getContext() {
            return ThreadContext.ASYNC;
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delayTicks, long intervalTicks) {
            Objects.requireNonNull(consumer, "consumer");
            RamTask task = new RamTask(consumer);
            task.setHandle(BACKEND.runRepeatingAsync(task, delayTicks, intervalTicks));
            return task;
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delay, @NotNull TimeUnit delayUnit, long interval, @NotNull TimeUnit intervalUnit) {
            Objects.requireNonNull(consumer, "consumer");
            RamTask task = new RamTask(consumer);
            task.setHandle(BACKEND.runRepeatingAsync(task, delay, delayUnit, interval, intervalUnit));
            return task;
        }
    }

    private abstract static class TargetScheduler implements Scheduler {

        @NotNull
        @Override
        public ThreadContext getContext() {
            return ThreadContext.SYNC;
        }

        @NotNull
        @Override
        public <T> Promise<T> supply(@NotNull Supplier<T> supplier) {
            Objects.requireNonNull(supplier, "supplier");
            return call(supplier::get);
        }

        @NotNull
        @Override
        public <T> Promise<T> call(@NotNull Callable<T> callable) {
            return callLater(callable, 0L);
        }

        @NotNull
        @Override
        public Promise<Void> run(@NotNull Runnable runnable) {
            Objects.requireNonNull(runnable, "runnable");
            return call(() -> {
                runnable.run();
                return null;
            });
        }

        @NotNull
        @Override
        public <T> Promise<T> supplyLater(@NotNull Supplier<T> supplier, long delayTicks) {
            Objects.requireNonNull(supplier, "supplier");
            return callLater(supplier::get, delayTicks);
        }

        @NotNull
        @Override
        public <T> Promise<T> supplyLater(@NotNull Supplier<T> supplier, long delay, @NotNull TimeUnit unit) {
            Objects.requireNonNull(unit, "unit");
            return supplyLater(supplier, Ticks.from(delay, unit));
        }

        @NotNull
        @Override
        public <T> Promise<T> callLater(@NotNull Callable<T> callable, long delayTicks) {
            Objects.requireNonNull(callable, "callable");
            Promise<T> promise = Promise.empty();
            schedulePromise(promise, callable, delayTicks);
            return promise;
        }

        @NotNull
        @Override
        public <T> Promise<T> callLater(@NotNull Callable<T> callable, long delay, @NotNull TimeUnit unit) {
            Objects.requireNonNull(unit, "unit");
            return callLater(callable, Ticks.from(delay, unit));
        }

        @NotNull
        @Override
        public Promise<Void> runLater(@NotNull Runnable runnable, long delayTicks) {
            Objects.requireNonNull(runnable, "runnable");
            return callLater(() -> {
                runnable.run();
                return null;
            }, delayTicks);
        }

        @NotNull
        @Override
        public Promise<Void> runLater(@NotNull Runnable runnable, long delay, @NotNull TimeUnit unit) {
            Objects.requireNonNull(unit, "unit");
            return runLater(runnable, Ticks.from(delay, unit));
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delay, @NotNull TimeUnit delayUnit, long interval, @NotNull TimeUnit intervalUnit) {
            Objects.requireNonNull(delayUnit, "delayUnit");
            Objects.requireNonNull(intervalUnit, "intervalUnit");
            return runRepeating(consumer, Ticks.from(delay, delayUnit), Ticks.from(interval, intervalUnit));
        }

        protected abstract <T> void schedulePromise(@NotNull Promise<T> promise, @NotNull Callable<T> callable, long delayTicks);
    }

    private static final class EntityBoundScheduler extends TargetScheduler {
        private final Entity entity;

        private EntityBoundScheduler(Entity entity) {
            this.entity = Objects.requireNonNull(entity, "entity");
        }

        @Override
        public void execute(Runnable runnable) {
            BACKEND.executeEntity(this.entity, runnable, () -> {});
        }

        @Override
        protected <T> void schedulePromise(@NotNull Promise<T> promise, @NotNull Callable<T> callable, long delayTicks) {
            BACKEND.runDelayedEntity(this.entity, () -> completePromise(promise, callable), promise::cancel, delayTicks);
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delayTicks, long intervalTicks) {
            Objects.requireNonNull(consumer, "consumer");
            RamTask task = new RamTask(consumer);
            task.setHandle(BACKEND.runRepeatingEntity(this.entity, task, task::stop, delayTicks, intervalTicks));
            return task;
        }

    }

    private static final class RegionBoundScheduler extends TargetScheduler {
        private final Location location;

        private RegionBoundScheduler(Location location) {
            this.location = Objects.requireNonNull(location, "location").clone();
        }

        @Override
        public void execute(Runnable runnable) {
            BACKEND.executeRegion(this.location, runnable);
        }

        @Override
        protected <T> void schedulePromise(@NotNull Promise<T> promise, @NotNull Callable<T> callable, long delayTicks) {
            BACKEND.runDelayedRegion(this.location, () -> completePromise(promise, callable), delayTicks);
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delayTicks, long intervalTicks) {
            Objects.requireNonNull(consumer, "consumer");
            RamTask task = new RamTask(consumer);
            task.setHandle(BACKEND.runRepeatingRegion(this.location, task, delayTicks, intervalTicks));
            return task;
        }

    }

    private static final class ChunkBoundScheduler extends TargetScheduler {
        private final World world;
        private final int chunkX;
        private final int chunkZ;

        private ChunkBoundScheduler(World world, int chunkX, int chunkZ) {
            this.world = Objects.requireNonNull(world, "world");
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public void execute(Runnable runnable) {
            BACKEND.executeRegion(this.world, this.chunkX, this.chunkZ, runnable);
        }

        @Override
        protected <T> void schedulePromise(@NotNull Promise<T> promise, @NotNull Callable<T> callable, long delayTicks) {
            BACKEND.runDelayedRegion(this.world, this.chunkX, this.chunkZ, () -> completePromise(promise, callable), delayTicks);
        }

        @NotNull
        @Override
        public Task runRepeating(@NotNull Consumer<Task> consumer, long delayTicks, long intervalTicks) {
            Objects.requireNonNull(consumer, "consumer");
            RamTask task = new RamTask(consumer);
            task.setHandle(BACKEND.runRepeatingRegion(this.world, this.chunkX, this.chunkZ, task, delayTicks, intervalTicks));
            return task;
        }

    }

    private static class RamTask implements Runnable, Task, Delegate<Consumer<Task>> {
        private final Consumer<Task> backingTask;

        private final AtomicInteger counter = new AtomicInteger(0);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicReference<SchedulerBackend.TaskHandle> handle = new AtomicReference<>();

        private RamTask(Consumer<Task> backingTask) {
            this.backingTask = backingTask;
        }

        private void setHandle(SchedulerBackend.TaskHandle handle) {
            this.handle.set(Objects.requireNonNull(handle, "handle"));
            if (this.cancelled.get()) {
                handle.cancel();
            }
        }

        @Override
        public void run() {
            if (this.cancelled.get()) {
                stop();
                return;
            }

            try {
                this.backingTask.accept(this);
                this.counter.incrementAndGet();
            } catch (Throwable e) {
                RamExceptions.reportScheduler(e);
            }

            if (this.cancelled.get()) {
                stop();
            }
        }

        @Override
        public int getTimesRan() {
            return this.counter.get();
        }

        @Override
        public boolean stop() {
            if (this.cancelled.getAndSet(true)) {
                return false;
            }

            SchedulerBackend.TaskHandle taskHandle = this.handle.get();
            if (taskHandle != null) {
                taskHandle.cancel();
            }
            return true;
        }

        @Override
        public int getBukkitId() {
            SchedulerBackend.TaskHandle taskHandle = this.handle.get();
            if (taskHandle == null) {
                return -1;
            }
            return taskHandle.getBukkitId();
        }

        @Override
        public boolean isClosed() {
            return this.cancelled.get();
        }

        @Override
        public Consumer<Task> getDelegate() {
            return this.backingTask;
        }
    }

    private Schedulers() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

}
