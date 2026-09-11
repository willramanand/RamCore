package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ContentLoaderTest {

    private static Path type(Path root, String type) throws Exception {
        Path dir = root.resolve(type);
        Files.createDirectories(dir);
        return dir;
    }

    private static void write(Path dir, String name, String... lines) throws Exception {
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    public void loadsSingleAndListEntries(@TempDir Path root) throws Exception {
        Path items = type(root, "items");
        write(items, "sword.yml",
                "id: test:sword",
                "material: DIAMOND_SWORD",
                "name: Sword");
        write(items, "misc.yml",
                "- id: test:apple",
                "  material: APPLE",
                "- id: test:bread",
                "  material: BREAD");

        ContentLoadResult result = ContentLoader.load(root);

        assertTrue(result.successful(), () -> result.errors().toString());
        assertEquals(3, result.definitions().size());
        assertEquals(3, result.ofType("items").size());
        ContentDefinition sword = result.definition(ContentId.parse("test:sword")).orElseThrow();
        assertEquals("items", sword.type());
        assertEquals("DIAMOND_SWORD", sword.node().node("material").getString());
    }

    @Test
    public void inheritanceDeepMerges(@TempDir Path root) throws Exception {
        Path items = type(root, "items");
        write(items, "base.yml",
                "id: test:base",
                "material: STICK",
                "name: Base",
                "stats:",
                "  a: 1",
                "  b: 2",
                "lore:",
                "  - one",
                "  - two");
        write(items, "child.yml",
                "id: test:child",
                "extends: test:base",
                "name: Child",
                "stats:",
                "  b: 3",
                "  c: 4",
                "lore:",
                "  - only");

        ContentLoadResult result = ContentLoader.load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        ContentDefinition child = result.definition(ContentId.parse("test:child")).orElseThrow();
        assertEquals("STICK", child.node().node("material").getString(), "inherited scalar");
        assertEquals("Child", child.node().node("name").getString(), "child scalar wins");
        assertEquals(1, child.node().node("stats", "a").getInt(-1), "inherited map key");
        assertEquals(3, child.node().node("stats", "b").getInt(-1), "child map key wins");
        assertEquals(4, child.node().node("stats", "c").getInt(-1), "child-only map key");
        assertEquals(List.of("only"), child.node().node("lore").getList(String.class), "child list replaces");
    }

    @Test
    public void missingParentIsReportedWithSourceAndExcludesDefinition(@TempDir Path root) throws Exception {
        Path items = type(root, "items");
        write(items, "orphan.yml", "id: test:orphan", "extends: test:ghost");

        ContentLoadResult result = ContentLoader.load(root);
        assertFalse(result.successful());
        assertEquals(1, result.errors().size());
        ValidationError error = result.errors().get(0);
        assertEquals("orphan.yml", error.source());
        assertTrue(error.message().contains("missing parent test:ghost"));
        assertTrue(result.definitions().isEmpty(), "an entry with a missing parent is not resolved");
    }

    @Test
    public void cycleIsReported(@TempDir Path root) throws Exception {
        Path items = type(root, "items");
        write(items, "a.yml", "id: test:a", "extends: test:b");
        write(items, "b.yml", "id: test:b", "extends: test:a");

        ContentLoadResult result = ContentLoader.load(root);
        assertFalse(result.successful());
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("cyclic inheritance")));
        assertTrue(result.definitions().isEmpty());
    }

    @Test
    public void duplicateAndMissingIdsAreBothReported(@TempDir Path root) throws Exception {
        Path items = type(root, "items");
        write(items, "dupe.yml",
                "- id: test:dupe",
                "  material: STONE",
                "- id: test:dupe",
                "  material: DIRT");
        write(items, "noid.yml", "material: AIR");

        ContentLoadResult result = ContentLoader.load(root);
        assertFalse(result.successful());
        assertEquals(2, result.errors().size(), () -> result.errors().toString());
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("duplicate id test:dupe")));
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("no 'id'")));
    }

    @Test
    public void everyErrorIsCollectedNotJustTheFirst(@TempDir Path root) throws Exception {
        Path items = type(root, "items");
        write(items, "bad.yml",
                "- id: not-a-valid-id-format-!!",
                "  x: 1",
                "- id: test:orphan",
                "  extends: test:missing");

        ContentLoadResult result = ContentLoader.load(root);
        assertFalse(result.successful());
        assertEquals(2, result.errors().size(), () -> result.errors().toString());
        assertThrows(ContentValidationException.class, result::throwIfErrors);
    }

    @Test
    public void nonDirectoryRootIsAnError(@TempDir Path root) throws Exception {
        Path file = root.resolve("not-a-dir.txt");
        Files.writeString(file, "hi");
        ContentLoadResult result = ContentLoader.load(file);
        assertFalse(result.successful());
        assertTrue(result.errors().get(0).message().contains("not a directory"));
    }
}
