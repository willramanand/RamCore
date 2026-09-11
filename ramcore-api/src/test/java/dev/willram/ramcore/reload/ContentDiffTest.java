package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentLoadResult;
import dev.willram.ramcore.content.ContentLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ContentDiffTest {

    private static void write(Path root, String name, String... lines) throws Exception {
        Path dir = root.resolve("widgets");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    private static ContentSnapshot snapshot(Path root) {
        return ContentSnapshot.of(ContentLoader.load(root));
    }

    @Test
    public void canonicalIsOrderIndependent() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("y", 2);
        b.put("x", 1);
        assertEquals(ContentHashing.canonical(a), ContentHashing.canonical(b));
    }

    @Test
    public void detectsAddedChangedRemoved(@TempDir Path root) throws Exception {
        write(root, "a.yml", "id: test:a", "x: 1");
        ContentSnapshot first = snapshot(root);

        write(root, "a.yml", "id: test:a", "x: 2");   // change a
        write(root, "b.yml", "id: test:b", "y: 9");    // add b
        ContentLoadResult result = ContentLoader.load(root);
        ContentSnapshot second = ContentSnapshot.of(result);

        ContentDiff diff = ContentDiff.data(first, second, result);
        assertTrue(diff.changed().contains(ContentId.parse("test:a")), () -> diff.changed().toString());
        assertTrue(diff.added().contains(ContentId.parse("test:b")));
        assertTrue(diff.removed().isEmpty());
        assertTrue(diff.dirty());

        // now remove b
        Files.delete(root.resolve("widgets").resolve("b.yml"));
        ContentLoadResult result3 = ContentLoader.load(root);
        ContentDiff diff3 = ContentDiff.data(second, ContentSnapshot.of(result3), result3);
        assertTrue(diff3.removed().contains(ContentId.parse("test:b")));
    }

    @Test
    public void unchangedContentHasStableHash(@TempDir Path root) throws Exception {
        write(root, "a.yml", "id: test:a", "x: 1", "nested:", "  k: v");
        ContentSnapshot first = snapshot(root);
        ContentSnapshot again = snapshot(root);
        ContentDiff diff = ContentDiff.data(first, again, ContentLoader.load(root));
        assertFalse(diff.dirty());
        assertEquals(first.hash(ContentId.parse("test:a")), again.hash(ContentId.parse("test:a")));
    }
}
