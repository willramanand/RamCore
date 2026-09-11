package dev.willram.ramcore.world;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * One block inside a structure snapshot.
 */
public record StructureBlockSnapshot(
        @NotNull BlockOffset offset,
        @NotNull String material,
        @NotNull String blockData,
        @NotNull Optional<BlockEntitySnapshot> blockEntity
) {

    public StructureBlockSnapshot {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(blockData, "blockData");
        Objects.requireNonNull(blockEntity, "blockEntity");
    }

    @NotNull
    public static StructureBlockSnapshot of(@NotNull BlockOffset offset, @NotNull String material, @NotNull String blockData) {
        return new StructureBlockSnapshot(offset, material, blockData, Optional.empty());
    }

    @NotNull
    public StructureBlockSnapshot withBlockEntity(@NotNull BlockEntitySnapshot blockEntity) {
        return new StructureBlockSnapshot(this.offset, this.material, this.blockData, Optional.of(blockEntity));
    }
}
