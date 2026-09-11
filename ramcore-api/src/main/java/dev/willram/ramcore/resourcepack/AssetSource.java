package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A source of bytes for one file inside a resource pack (a texture, a model, {@code sounds.json},
 * ...). Read lazily by {@link ResourcePackBuilder} when the pack is written, so a source may point at
 * a jar resource or a file on disk without loading it up front.
 */
@FunctionalInterface
public interface AssetSource {

    /**
     * The file's bytes.
     *
     * @return the bytes
     * @throws IOException if the source cannot be read
     */
    byte @NotNull [] bytes() throws IOException;

    /** A source over the given bytes (defensively copied). */
    @NotNull
    static AssetSource ofBytes(byte @NotNull [] bytes) {
        byte[] copy = bytes.clone();
        return copy::clone;
    }

    /** A source over a UTF-8 string. */
    @NotNull
    static AssetSource ofString(@NotNull String content) {
        requireNonNull(content, "content");
        return ofBytes(content.getBytes(StandardCharsets.UTF_8));
    }

    /** A source that reads a file on disk each time it is requested. */
    @NotNull
    static AssetSource ofFile(@NotNull Path path) {
        requireNonNull(path, "path");
        return () -> Files.readAllBytes(path);
    }

    /** A source that reads a classpath resource each time it is requested. */
    @NotNull
    static AssetSource ofResource(@NotNull ClassLoader loader, @NotNull String name) {
        requireNonNull(loader, "loader");
        requireNonNull(name, "name");
        return () -> {
            try (InputStream in = loader.getResourceAsStream(name)) {
                if (in == null) {
                    throw new IOException("resource not found: " + name);
                }
                return in.readAllBytes();
            }
        };
    }
}
