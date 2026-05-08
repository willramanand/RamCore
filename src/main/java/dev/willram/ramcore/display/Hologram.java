package dev.willram.ramcore.display;

import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Spawned hologram display stack.
 */
public final class Hologram implements Terminable {
    private final List<DisplayHandle<TextDisplay>> lines;
    private final CompositeTerminable terminables;

    Hologram(@NotNull List<DisplayHandle<TextDisplay>> lines) {
        this.lines = List.copyOf(lines);
        this.terminables = CompositeTerminable.create().withAll(lines);
    }

    @NotNull
    public List<DisplayHandle<TextDisplay>> lines() {
        return this.lines;
    }

    public void teleport(@NotNull Location base, double lineSpacing) {
        for (int i = 0; i < this.lines.size(); i++) {
            this.lines.get(i).teleport(base.clone().add(0.0d, -(i * lineSpacing), 0.0d));
        }
    }

    @Override
    public void close() {
        this.terminables.closeAndReportException();
    }

    @Override
    public boolean isClosed() {
        return this.lines.stream().allMatch(DisplayHandle::isClosed);
    }
}
