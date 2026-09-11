package dev.willram.ramcore.content;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Where a {@link ContentDefinition} came from: the file and the path within it.
 *
 * @param file the file name (relative or bare), for use in error messages
 * @param path the path within the file, such as a list index or entry id; may be empty
 */
public record SourceRef(@NotNull String file, @NotNull String path) {

    public SourceRef {
        requireNonNull(file, "file");
        requireNonNull(path, "path");
    }

    @NotNull
    public static SourceRef of(@NotNull String file, @Nullable String path) {
        return new SourceRef(file, path == null ? "" : path);
    }

    @Override
    public String toString() {
        return this.path.isEmpty() ? this.file : this.file + "#" + this.path;
    }
}
