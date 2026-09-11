package dev.willram.ramcore.worldinstance;

import dev.willram.ramcore.promise.Promise;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * The platform seam for instanced worlds: whether the server supports runtime world creation, where
 * worlds live, and how to load/unload/evacuate one. A Paper implementation uses {@code Bukkit.createWorld}
 * on the global scheduler; an in-memory fake makes the service's copy/marker/sweep logic unit-testable.
 */
public interface WorldBackend {

    /** Whether the server supports creating worlds at runtime (false on regionised servers like Folia). */
    boolean supportsInstances();

    /** The server's world container directory (instances are created as subdirectories). */
    @NotNull
    Path worldContainer();

    /**
     * Loads a prepared world directory into the server (on the global region).
     *
     * @param name the world/directory name
     * @return a promise completing when loaded
     */
    @NotNull
    Promise<Void> loadWorld(@NotNull String name);

    /**
     * Unloads a world.
     *
     * @param name the world name
     * @param save whether to save before unloading
     * @return a promise completing when unloaded
     */
    @NotNull
    Promise<Void> unloadWorld(@NotNull String name, boolean save);

    /**
     * Teleports any players out of the world (each on its own scheduler).
     *
     * @param name the world name
     * @return a promise completing when evacuated
     */
    @NotNull
    Promise<Void> evacuate(@NotNull String name);
}
