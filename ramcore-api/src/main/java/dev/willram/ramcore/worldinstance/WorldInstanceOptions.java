package dev.willram.ramcore.worldinstance;

import org.jetbrains.annotations.NotNull;

/**
 * Options for creating a world instance.
 *
 * @param deleteOnClose whether the instance directory is deleted when the instance closes
 * @param saveOnClose   whether the world is saved before it is unloaded
 */
public record WorldInstanceOptions(boolean deleteOnClose, boolean saveOnClose) {

    /** Delete on close, do not save (the common ephemeral-dungeon case). */
    @NotNull
    public static WorldInstanceOptions defaults() {
        return new WorldInstanceOptions(true, false);
    }
}
