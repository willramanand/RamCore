package dev.willram.ramcore.world;

import dev.willram.ramcore.serialize.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Relative block coordinate inside a structure snapshot.
 */
public record BlockOffset(int x, int y, int z) {

    @NotNull
    public BlockPosition apply(@NotNull BlockPosition origin) {
        Objects.requireNonNull(origin, "origin");
        return origin.add(this.x, this.y, this.z);
    }

    @NotNull
    public static BlockOffset between(@NotNull BlockPosition origin, @NotNull BlockPosition position) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(position, "position");
        if (!origin.getWorld().equals(position.getWorld())) {
            throw new IllegalArgumentException("positions are in different worlds");
        }
        return new BlockOffset(position.getX() - origin.getX(), position.getY() - origin.getY(), position.getZ() - origin.getZ());
    }
}
