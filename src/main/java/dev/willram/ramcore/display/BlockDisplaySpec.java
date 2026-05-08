package dev.willram.ramcore.display;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Block display entity configuration.
 */
public final class BlockDisplaySpec implements DisplaySpec<BlockDisplay> {
    private final DisplayOptions options = new DisplayOptions();
    private BlockData blockData;

    @NotNull
    public static BlockDisplaySpec block(@NotNull BlockData blockData) {
        return new BlockDisplaySpec().blockData(blockData);
    }

    @NotNull
    public BlockDisplaySpec blockData(@NotNull BlockData blockData) {
        this.blockData = requireNonNull(blockData, "blockData");
        return this;
    }

    @NotNull
    public DisplayOptions options() {
        return this.options;
    }

    @NotNull
    @Override
    public Class<BlockDisplay> type() {
        return BlockDisplay.class;
    }

    @Override
    public void apply(@NotNull BlockDisplay display) {
        this.options.apply(display);
        if (this.blockData != null) {
            display.setBlock(this.blockData);
        }
    }
}
