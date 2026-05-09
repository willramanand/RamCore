package dev.willram.ramcore.world;

import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Boundary for raw block-entity SNBT import/export.
 */
public interface BlockEntityNbtCodec {

    boolean supported();

    @NotNull
    BlockEntityNbtResult<String> exportSnbt(@NotNull BlockState state);

    @NotNull
    BlockEntityNbtResult<BlockState> importSnbt(@NotNull BlockState state, @NotNull String snbt);
}
