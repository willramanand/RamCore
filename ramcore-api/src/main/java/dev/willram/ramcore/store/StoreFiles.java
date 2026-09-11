package dev.willram.ramcore.store;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * File primitives shared by {@link FileStore} and the legacy {@code FileDataRepository}: atomic
 * writes through a sibling temp file, and directory listing by extension.
 */
public final class StoreFiles {
    private static final String TEMP_SUFFIX = ".tmp";

    private StoreFiles() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /** A write callback that may throw. */
    @FunctionalInterface
    public interface WriterAction {
        void write(@NotNull Writer writer) throws IOException;
    }

    /**
     * Reads a UTF-8 file.
     *
     * @param path the file
     * @return its contents
     * @throws IOException on read failure
     */
    @NotNull
    public static String readString(@NotNull Path path) throws IOException {
        return Files.readString(Objects.requireNonNull(path, "path"), StandardCharsets.UTF_8);
    }

    /**
     * Writes a UTF-8 file atomically: the content goes to {@code <file>.tmp} first and is then
     * moved over the target, so a crash never leaves a half-written file.
     *
     * @param path    the target file; parent directories are created
     * @param content the content
     * @throws IOException on write failure
     */
    public static void writeAtomically(@NotNull Path path, @NotNull String content) throws IOException {
        Objects.requireNonNull(content, "content");
        writeAtomically(path, writer -> writer.write(content));
    }

    /**
     * Writes a UTF-8 file atomically through a writer callback.
     *
     * @param path   the target file; parent directories are created
     * @param action writes the content
     * @throws IOException on write failure
     */
    public static void writeAtomically(@NotNull Path path, @NotNull WriterAction action) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(action, "action");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = path.resolveSibling(path.getFileName() + TEMP_SUFFIX);
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            action.write(writer);
        }
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Lists the file names in a directory with the given extension, extension stripped. Temp files
     * left by an interrupted write are ignored. A missing directory yields an empty list.
     *
     * @param directory the directory
     * @param extension the extension including the dot, for example {@code .json}
     * @return the bare names
     * @throws IOException on listing failure
     */
    @NotNull
    public static List<String> listNames(@NotNull Path directory, @NotNull String extension) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(extension, "extension");
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(extension) && !fileName.endsWith(TEMP_SUFFIX)) {
                    names.add(fileName.substring(0, fileName.length() - extension.length()));
                }
            }
        }
        return names;
    }
}
