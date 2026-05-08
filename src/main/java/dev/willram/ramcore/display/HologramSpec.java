package dev.willram.ramcore.display;

import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Text-display hologram stack specification.
 */
public final class HologramSpec {
    private final List<HologramLine> lines = new ArrayList<>();
    private double lineSpacing = 0.28d;

    @NotNull
    public static HologramSpec create() {
        return new HologramSpec();
    }

    @NotNull
    public HologramSpec line(@NotNull HologramLine line) {
        this.lines.add(line);
        return this;
    }

    @NotNull
    public HologramSpec text(@NotNull ComponentLike text) {
        return line(HologramLine.text(text));
    }

    @NotNull
    public HologramSpec lineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    @NotNull
    public List<HologramLine> lines() {
        return List.copyOf(this.lines);
    }

    public double lineSpacing() {
        return this.lineSpacing;
    }
}
