package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads and writes the per-file hash manifest used for incremental builds: one line per file,
 * {@code <sha1hex><tab><path>}, sorted by path. A line-based format is used rather than JSON so the
 * builder needs no JSON parser (Gson is server-provided and off-limits off-server).
 */
final class PackManifest {

    private PackManifest() {
    }

    /** Reads a manifest, or an empty map if the file does not exist. */
    @NotNull
    static Map<String, String> read(@NotNull Path manifest) throws IOException {
        Map<String, String> out = new TreeMap<>();
        if (!Files.exists(manifest)) {
            return out;
        }
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab <= 0) {
                continue;
            }
            out.put(line.substring(tab + 1), line.substring(0, tab));
        }
        return out;
    }

    /** Writes a manifest (path → sha1hex), sorted by path. */
    static void write(@NotNull Path manifest, @NotNull Map<String, String> hashesByPath) throws IOException {
        List<String> lines = new java.util.ArrayList<>(hashesByPath.size());
        new TreeMap<>(hashesByPath).forEach((path, hex) -> lines.add(hex + "\t" + path));
        Path parent = manifest.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(manifest, lines, StandardCharsets.UTF_8);
    }
}
