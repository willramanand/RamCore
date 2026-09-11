package dev.willram.ramcore.display;

import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Owned spawned display entity.
 */
public final class DisplayHandle<T extends Display> implements Terminable {
    private final T display;

    DisplayHandle(@NotNull T display) {
        this.display = requireNonNull(display, "display");
    }

    @NotNull
    public T display() {
        return this.display;
    }

    public boolean valid() {
        return this.display.isValid() && !this.display.isDead();
    }

    public void teleport(@NotNull Location location) {
        requireNonNull(location, "location");
        Schedulers.run(this.display, () -> this.display.teleport(location));
    }

    @Override
    public void close() {
        Schedulers.run(this.display, this.display::remove);
    }

    @Override
    public boolean isClosed() {
        return !valid();
    }
}
