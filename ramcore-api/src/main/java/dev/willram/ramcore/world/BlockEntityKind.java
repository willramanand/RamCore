package dev.willram.ramcore.world;

import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.block.Lectern;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Coarse block-entity surfaces that Paper exposes through typed block states.
 */
public enum BlockEntityKind {
    SIGN,
    CONTAINER,
    SPAWNER,
    SKULL,
    BANNER,
    LECTERN,
    COMMAND_BLOCK,
    TILE_STATE,
    BLOCK_STATE;

    @NotNull
    public static BlockEntityKind of(@NotNull BlockState state) {
        Objects.requireNonNull(state, "state");
        if (state instanceof Sign) {
            return SIGN;
        }
        if (state instanceof Container) {
            return CONTAINER;
        }
        if (state instanceof CreatureSpawner) {
            return SPAWNER;
        }
        if (state instanceof Skull) {
            return SKULL;
        }
        if (state instanceof Banner) {
            return BANNER;
        }
        if (state instanceof Lectern) {
            return LECTERN;
        }
        if (state instanceof CommandBlock) {
            return COMMAND_BLOCK;
        }
        if (state instanceof TileState) {
            return TILE_STATE;
        }
        return BLOCK_STATE;
    }

    public boolean blockEntity() {
        return this != BLOCK_STATE;
    }
}
