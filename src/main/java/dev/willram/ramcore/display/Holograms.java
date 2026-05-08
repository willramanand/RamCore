package dev.willram.ramcore.display;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Region-safe hologram spawning helpers.
 */
public final class Holograms {

    @NotNull
    public static Promise<Hologram> spawn(@NotNull Location base, @NotNull HologramSpec spec) {
        requireNonNull(base, "base");
        requireNonNull(spec, "spec");
        World world = requireNonNull(base.getWorld(), "base world");
        Location spawnBase = base.clone();
        return Schedulers.call(spawnBase, () -> {
            List<DisplayHandle<TextDisplay>> handles = new ArrayList<>();
            List<HologramLine> lines = spec.lines();
            for (int i = 0; i < lines.size(); i++) {
                HologramLine line = lines.get(i);
                Location location = spawnBase.clone().add(0.0d, line.verticalOffset() - (i * spec.lineSpacing()), 0.0d);
                TextDisplay display = world.spawn(location, TextDisplay.class, line.spec()::apply);
                handles.add(new DisplayHandle<>(display));
            }
            return new Hologram(handles);
        });
    }

    private Holograms() {
    }
}
