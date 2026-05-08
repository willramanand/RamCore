package dev.willram.ramcore.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Declares the execution anchor for scheduler work.
 *
 * <p>Consumers provide intent through this type; RamCore chooses the correct
 * Paper/Folia scheduler for that anchor.</p>
 */
public final class TaskContext {

    private static final TaskContext GLOBAL = new TaskContext(Type.GLOBAL, null, null);
    private static final TaskContext ASYNC = new TaskContext(Type.ASYNC, null, null);

    private final Type type;
    private final Entity entity;
    private final Location location;
    private final World world;
    private final int chunkX;
    private final int chunkZ;

    private TaskContext(Type type, Entity entity, Location location) {
        this(type, entity, location, null, 0, 0);
    }

    private TaskContext(Type type, Entity entity, Location location, World world, int chunkX, int chunkZ) {
        this.type = type;
        this.entity = entity;
        this.location = location;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @NotNull
    public static TaskContext global() {
        return GLOBAL;
    }

    @NotNull
    public static TaskContext sync() {
        return global();
    }

    @NotNull
    public static TaskContext async() {
        return ASYNC;
    }

    @NotNull
    public static TaskContext entity(@NotNull Entity entity) {
        return of(entity);
    }

    @NotNull
    public static TaskContext of(@NotNull Entity entity) {
        return new TaskContext(Type.ENTITY, Objects.requireNonNull(entity, "entity"), null);
    }

    @NotNull
    public static TaskContext player(@NotNull Player player) {
        return of(player);
    }

    @NotNull
    public static TaskContext of(@NotNull Player player) {
        return of((Entity) player);
    }

    @NotNull
    public static TaskContext region(@NotNull Location location) {
        return of(location);
    }

    @NotNull
    public static TaskContext of(@NotNull Location location) {
        return new TaskContext(Type.REGION, null, Objects.requireNonNull(location, "location").clone());
    }

    @NotNull
    public static TaskContext block(@NotNull Block block) {
        return of(block);
    }

    @NotNull
    public static TaskContext of(@NotNull Block block) {
        return of(Objects.requireNonNull(block, "block").getLocation());
    }

    @NotNull
    public static TaskContext blockState(@NotNull BlockState blockState) {
        return of(blockState);
    }

    @NotNull
    public static TaskContext of(@NotNull BlockState blockState) {
        return of(Objects.requireNonNull(blockState, "blockState").getLocation());
    }

    @NotNull
    public static TaskContext chunk(@NotNull Chunk chunk) {
        return of(chunk);
    }

    @NotNull
    public static TaskContext of(@NotNull Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return of(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @NotNull
    public static TaskContext chunk(@NotNull World world, int chunkX, int chunkZ) {
        return of(world, chunkX, chunkZ);
    }

    @NotNull
    public static TaskContext of(@NotNull World world, int chunkX, int chunkZ) {
        return new TaskContext(Type.CHUNK, null, null, Objects.requireNonNull(world, "world"), chunkX, chunkZ);
    }

    @NotNull
    public Type type() {
        return this.type;
    }

    @Nullable
    public Entity entity() {
        return this.entity;
    }

    @Nullable
    public Location location() {
        return this.location;
    }

    @Nullable
    public World world() {
        return this.world;
    }

    public int chunkX() {
        return this.chunkX;
    }

    public int chunkZ() {
        return this.chunkZ;
    }

    public boolean globalContext() {
        return this.type == Type.GLOBAL;
    }

    public boolean asyncContext() {
        return this.type == Type.ASYNC;
    }

    public boolean entityContext() {
        return this.type == Type.ENTITY;
    }

    public boolean regionContext() {
        return this.type == Type.REGION;
    }

    public boolean chunkContext() {
        return this.type == Type.CHUNK;
    }

    @NotNull
    public String description() {
        return switch (this.type) {
            case GLOBAL -> "global";
            case ASYNC -> "async";
            case ENTITY -> "entity:" + this.entity.getUniqueId();
            case REGION -> "region:" + this.location.getWorld().getName() + "@"
                    + this.location.getBlockX() + ","
                    + this.location.getBlockY() + ","
                    + this.location.getBlockZ();
            case CHUNK -> "chunk:" + this.world.getName() + "@" + this.chunkX + "," + this.chunkZ;
        };
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof TaskContext that)) {
            return false;
        }

        return this.chunkX == that.chunkX
                && this.chunkZ == that.chunkZ
                && this.type == that.type
                && Objects.equals(this.entity, that.entity)
                && Objects.equals(this.location, that.location)
                && Objects.equals(this.world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.entity, this.location, this.world, this.chunkX, this.chunkZ);
    }

    @Override
    public String toString() {
        return "TaskContext[" + description() + "]";
    }

    public enum Type {
        GLOBAL,
        ASYNC,
        ENTITY,
        REGION,
        CHUNK
    }
}
