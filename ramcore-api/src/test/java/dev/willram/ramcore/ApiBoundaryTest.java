package dev.willram.ramcore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Belt-and-braces boundary check for the {@code ramcore-api} module: its sources must not import
 * implementation-only packages that live in other modules ({@code ramcore-protocol},
 * {@code ramcore-nms}) or raw platform internals. The module structure enforces this at compile
 * time already (api has no dependency on those modules); this test keeps the intent explicit and
 * catches a stray import before it becomes a cross-module leak.
 */
public final class ApiBoundaryTest {
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java", "dev", "willram", "ramcore");

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "com.comphenix.",
            "net.minecraft.",
            "dev.willram.ramcore.protocol.",
            "dev.willram.ramcore.packet.",
            "dev.willram.ramcore.scoreboard.",
            "dev.willram.ramcore.shadows.",
            "dev.willram.ramcore.nbt.",
            "dev.willram.ramcore.nms.reflect."
    );

    @Test
    public void apiModuleDoesNotImportImplementationModules() throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT), "test must run from the ramcore-api module: " + SOURCE_ROOT.toAbsolutePath());
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
                for (String line : Files.readAllLines(file)) {
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    for (String prefix : FORBIDDEN_IMPORT_PREFIXES) {
                        if (line.contains(prefix)) {
                            violations.add(relative + ": " + line.trim());
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "ramcore-api imports implementation modules:\n  " + String.join("\n  ", violations));
    }
}
