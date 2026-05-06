package dev.willram.ramcore.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
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
    public static TaskContext async() {
        return ASYNC;
    }

    @NotNull
    public static TaskContext of(@NotNull Entity entity) {
        return new TaskContext(Type.ENTITY, Objects.requireNonNull(entity, "entity"), null);
    }

    @NotNull
    public static TaskContext of(@NotNull Player player) {
        return of((Entity) player);
    }

    @NotNull
    public static TaskContext of(@NotNull Location location) {
        return new TaskContext(Type.REGION, null, Objects.requireNonNull(location, "location").clone());
    }

    @NotNull
    public static TaskContext of(@NotNull Block block) {
        return of(Objects.requireNonNull(block, "block").getLocation());
    }

    @NotNull
    public static TaskContext of(@NotNull BlockState blockState) {
        return of(Objects.requireNonNull(blockState, "blockState").getLocation());
    }

    @NotNull
    public static TaskContext of(@NotNull Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return of(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @NotNull
    public static TaskContext of(@NotNull World world, int chunkX, int chunkZ) {
        return new TaskContext(Type.CHUNK, null, null, Objects.requireNonNull(world, "world"), chunkX, chunkZ);
    }

    Type type() {
        return this.type;
    }

    Entity entity() {
        return this.entity;
    }

    Location location() {
        return this.location;
    }

    World world() {
        return this.world;
    }

    int chunkX() {
        return this.chunkX;
    }

    int chunkZ() {
        return this.chunkZ;
    }

    enum Type {
        GLOBAL,
        ASYNC,
        ENTITY,
        REGION,
        CHUNK
    }
}
