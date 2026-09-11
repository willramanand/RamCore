package dev.willram.ramcore.reload;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A named, reloadable set of content: a name, the content root to (re)load, and a {@link ContentResolver}
 * that turns a reloaded definition into a domain object for rebinding.
 *
 * @param name     the pack name (used by {@code /ramcore diagnostics reload <name>})
 * @param root     the content root passed to {@code ContentLoader.load}
 * @param resolver builds the domain object for a reloaded definition
 */
public record ContentPack(@NotNull String name, @NotNull Path root, @NotNull ContentResolver resolver) {

    public ContentPack {
        RamPreconditions.notBlank(name, "name");
        requireNonNull(root, "root");
        requireNonNull(resolver, "resolver");
    }
}
