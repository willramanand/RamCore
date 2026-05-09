package dev.willram.ramcore.world;

import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class UnsupportedBlockEntityNbtCodec implements BlockEntityNbtCodec {
    private final String message;

    UnsupportedBlockEntityNbtCodec(@NotNull String message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public boolean supported() {
        return false;
    }

    @NotNull
    @Override
    public BlockEntityNbtResult<String> exportSnbt(@NotNull BlockState state) {
        Objects.requireNonNull(state, "state");
        return BlockEntityNbtResult.unsupported(this.message);
    }

    @NotNull
    @Override
    public BlockEntityNbtResult<BlockState> importSnbt(@NotNull BlockState state, @NotNull String snbt) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(snbt, "snbt");
        return BlockEntityNbtResult.unsupported(this.message);
    }
}
