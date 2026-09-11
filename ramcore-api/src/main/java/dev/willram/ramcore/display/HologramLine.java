package dev.willram.ramcore.display;

import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

/**
 * One text-display line in a hologram stack.
 */
public record HologramLine(
        @NotNull TextDisplaySpec spec,
        double verticalOffset
) {

    @NotNull
    public static HologramLine text(@NotNull ComponentLike text) {
        return new HologramLine(TextDisplaySpec.text(text), 0.0d);
    }

    @NotNull
    public HologramLine offset(double verticalOffset) {
        return new HologramLine(this.spec, verticalOffset);
    }
}
