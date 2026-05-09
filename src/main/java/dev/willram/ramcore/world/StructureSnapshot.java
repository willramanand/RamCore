package dev.willram.ramcore.world;

import dev.willram.ramcore.serialize.BlockPosition;
import dev.willram.ramcore.serialize.BlockRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Region snapshot with relative block coordinates.
 */
public record StructureSnapshot(@NotNull BlockPosition origin, @NotNull List<StructureBlockSnapshot> blocks) {

    public StructureSnapshot {
        Objects.requireNonNull(origin, "origin");
        blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
    }

    @NotNull
    public static Builder builder(@NotNull BlockPosition origin) {
        return new Builder(origin);
    }

    @NotNull
    public static StructureSnapshot capture(@NotNull BlockRegion region) {
        Objects.requireNonNull(region, "region");
        BlockPosition origin = region.getMin();
        List<StructureBlockSnapshot> blocks = new ArrayList<>();
        for (int y = region.getMin().getY(); y <= region.getMax().getY(); y++) {
            for (int z = region.getMin().getZ(); z <= region.getMax().getZ(); z++) {
                for (int x = region.getMin().getX(); x <= region.getMax().getX(); x++) {
                    BlockPosition position = BlockPosition.of(x, y, z, origin.getWorld());
                    Block block = position.toBlock();
                    BlockState state = block.getState();
                    StructureBlockSnapshot snapshot = StructureBlockSnapshot.of(
                            BlockOffset.between(origin, position),
                            state.getType().key().asString(),
                            state.getBlockData().getAsString()
                    );
                    if (BlockEntityKind.of(state).blockEntity()) {
                        snapshot = snapshot.withBlockEntity(BlockEntitySnapshot.capture(state));
                    }
                    blocks.add(snapshot);
                }
            }
        }
        return new StructureSnapshot(origin, blocks);
    }

    public void validateSingleTargetChunk(@NotNull BlockPosition targetOrigin) {
        Objects.requireNonNull(targetOrigin, "targetOrigin");
        int chunkX = targetOrigin.getX() >> 4;
        int chunkZ = targetOrigin.getZ() >> 4;
        for (StructureBlockSnapshot block : this.blocks) {
            BlockPosition position = block.offset().apply(targetOrigin);
            if ((position.getX() >> 4) != chunkX || (position.getZ() >> 4) != chunkZ) {
                throw new IllegalArgumentException("Structure restore crosses chunk boundaries; split the snapshot or disable requireSingleChunk after scheduling each affected region explicitly.");
            }
        }
    }

    @NotNull
    public StructureApplyResult restore(@NotNull BlockPosition targetOrigin, @NotNull StructureApplyOptions options) {
        Objects.requireNonNull(targetOrigin, "targetOrigin");
        Objects.requireNonNull(options, "options");
        if (options.requireSingleChunk()) {
            validateSingleTargetChunk(targetOrigin);
        }
        World world = Objects.requireNonNull(Bukkit.getWorld(targetOrigin.getWorld()), "target world");
        int blockEntities = 0;
        int skippedBlockEntities = 0;
        for (StructureBlockSnapshot snapshot : sortedBlocks()) {
            BlockPosition position = snapshot.offset().apply(targetOrigin);
            Block block = world.getBlockAt(position.getX(), position.getY(), position.getZ());
            block.setBlockData(Bukkit.createBlockData(snapshot.blockData()), options.physics());
            if (snapshot.blockEntity().isPresent()) {
                if (options.applyBlockEntities()) {
                    blockEntities++;
                } else {
                    skippedBlockEntities++;
                }
            }
        }
        return new StructureApplyResult(this.blocks.size(), blockEntities, skippedBlockEntities);
    }

    @NotNull
    private List<StructureBlockSnapshot> sortedBlocks() {
        return this.blocks.stream()
                .sorted(Comparator.comparingInt((StructureBlockSnapshot block) -> block.offset().y())
                        .thenComparingInt(block -> block.offset().z())
                        .thenComparingInt(block -> block.offset().x()))
                .toList();
    }

    public static final class Builder {
        private final BlockPosition origin;
        private final List<StructureBlockSnapshot> blocks = new ArrayList<>();

        private Builder(BlockPosition origin) {
            this.origin = Objects.requireNonNull(origin, "origin");
        }

        @NotNull
        public Builder block(@NotNull StructureBlockSnapshot block) {
            this.blocks.add(Objects.requireNonNull(block, "block"));
            return this;
        }

        @NotNull
        public StructureSnapshot build() {
            return new StructureSnapshot(this.origin, this.blocks);
        }
    }
}
