package dev.willram.ramcore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the module boundary from {@code docs/MODULE_BOUNDARIES.md} before the Maven split exists:
 * packages destined for {@code ramcore-api} must not import ProtocolLib, NMS, or the RamCore packages
 * that will live in {@code ramcore-protocol} / {@code ramcore-nms}.
 */
public final class ApiBoundaryTest {
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java", "dev", "willram", "ramcore");

    /** Package directories (relative to the source root) that are not part of the api module. */
    private static final Set<String> IMPLEMENTATION_PACKAGES = Set.of(
            "protocol",
            "packet",
            "scoreboard",
            "reflect",
            "shadows",
            "nbt",
            "nms/reflect",
            "event/functional/protocol"
    );

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "com.comphenix.",
            "net.minecraft.",
            "dev.willram.ramcore.protocol.",
            "dev.willram.ramcore.packet.",
            "dev.willram.ramcore.scoreboard.",
            "dev.willram.ramcore.reflect.",
            "dev.willram.ramcore.shadows.",
            "dev.willram.ramcore.nbt.",
            "dev.willram.ramcore.nms.reflect."
    );

    /**
     * Known violations scheduled for task 2.2 (module split). Each entry is a relative file path and
     * the import prefix it is allowed to keep until then. Remove entries as they are fixed.
     */
    private static final Set<String> EXEMPT_UNTIL_2_2 = Set.of(
            // moves to ramcore-protocol keeping its FQN
            "event/ProtocolSubscription.java|dev.willram.ramcore.protocol.",
            "event/ProtocolSubscription.java|com.comphenix.",
            // loaded through a ServiceLoader hook after the split
            "integration/ProtocolLibIntegrationProvider.java|dev.willram.ramcore.protocol.",
            "integration/ProtocolLibIntegrationProvider.java|com.comphenix.",
            // MinecraftVersion / NmsVersion / ServerReflection value types move into ramcore-api
            "nms/api/NmsAccess.java|dev.willram.ramcore.reflect.",
            "nms/api/NmsAccessRegistry.java|dev.willram.ramcore.reflect.",
            "nms/api/NmsCapabilityCheck.java|dev.willram.ramcore.reflect.",
            "nms/api/NmsCompatibilityCell.java|dev.willram.ramcore.reflect.",
            "nms/api/NmsCompatibilityMatrix.java|dev.willram.ramcore.reflect.",
            "nms/api/NmsDiagnostics.java|dev.willram.ramcore.reflect."
    );

    @Test
    public void apiPackagesDoNotImportImplementationModules() throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT), "test must run from the project root: " + SOURCE_ROOT.toAbsolutePath());
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
                if (isImplementationPackage(relative)) {
                    continue;
                }
                for (String line : Files.readAllLines(file)) {
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    for (String prefix : FORBIDDEN_IMPORT_PREFIXES) {
                        if (line.contains(prefix) && !EXEMPT_UNTIL_2_2.contains(relative + "|" + prefix)) {
                            violations.add(relative + ": " + line.trim());
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "api-destined packages import implementation modules:\n  " + String.join("\n  ", violations));
    }

    private static boolean isImplementationPackage(String relativePath) {
        for (String pkg : IMPLEMENTATION_PACKAGES) {
            if (relativePath.startsWith(pkg + "/")) {
                return true;
            }
        }
        return false;
    }
}
