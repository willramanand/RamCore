package dev.willram.ramcore.worldinstance;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * File helpers for instanced worlds: copy a world template, sweep leftover instance directories, and
 * delete a directory tree. Pure file I/O — run on the async scheduler; unit-tested off-server.
 */
public final class WorldInstances {

    /** Files that must not be copied from a world template (they identify the source world/session). */
    public static final Set<String> SKIPPED = Set.of("uid.dat", "session.lock");

    /** The directory-name prefix for instanced worlds. */
    public static final String PREFIX = "ramcore_inst_";

    private WorldInstances() {
    }

    /**
     * Copies a world template directory to a target, skipping {@link #SKIPPED} files.
     *
     * @param template the template directory
     * @param target   the destination directory
     * @throws IOException on I/O failure or a missing template
     */
    public static void copyTemplate(@NotNull Path template, @NotNull Path target) throws IOException {
        requireNonNull(template, "template");
        requireNonNull(target, "target");
        if (!Files.isDirectory(template)) {
            throw new IOException("world template not found: " + template);
        }
        try (Stream<Path> walk = Files.walk(template)) {
            for (Path source : (Iterable<Path>) walk::iterator) {
                if (Files.isRegularFile(source) && SKIPPED.contains(source.getFileName().toString())) {
                    continue;
                }
                Path relative = template.relativize(source);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Path parent = destination.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(source, destination);
                }
            }
        }
    }

    /**
     * Every directory under {@code container} that carries an {@link InstanceMarker} and matches the
     * instance prefix.
     *
     * @param container the server world container
     * @return the instance directories
     * @throws IOException on I/O failure
     */
    @NotNull
    public static List<Path> sweep(@NotNull Path container) throws IOException {
        requireNonNull(container, "container");
        List<Path> found = new ArrayList<>();
        if (!Files.isDirectory(container)) {
            return found;
        }
        try (Stream<Path> children = Files.list(container)) {
            for (Path child : (Iterable<Path>) children::iterator) {
                if (Files.isDirectory(child)
                        && child.getFileName().toString().startsWith(PREFIX)
                        && InstanceMarker.present(child)) {
                    found.add(child);
                }
            }
        }
        return found;
    }

    /**
     * Recursively deletes a directory tree. No-op if it does not exist.
     *
     * @param directory the directory
     * @throws IOException on I/O failure
     */
    public static void deleteRecursively(@NotNull Path directory) throws IOException {
        requireNonNull(directory, "directory");
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
