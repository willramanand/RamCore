package dev.willram.ramcore.display;

import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for a display entity.
 */
public interface DisplaySpec<T extends Display> {

    @NotNull
    Class<T> type();

    void apply(@NotNull T display);
}
