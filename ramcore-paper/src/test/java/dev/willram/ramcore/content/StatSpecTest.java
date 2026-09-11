package dev.willram.ramcore.content;

import dev.willram.ramcore.content.spec.StatSpec;
import dev.willram.ramcore.stat.Stat;
import dev.willram.ramcore.stat.StatFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StatSpecTest {

    private SpecLoader loader() {
        return SpecLoader.create().deserializer("stats", StatSpec::deserialize);
    }

    private static void write(Path root, String name, String... lines) throws Exception {
        Path dir = root.resolve("stats");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    public void statSpecRoundTrip(@TempDir Path root) throws Exception {
        write(root, "health.yml",
                "id: test:health",
                "base: 20.0",
                "min: 0.0",
                "max: 40.0",
                "format: INTEGER");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        StatSpec spec = result.get(ContentId.parse("test:health"), StatSpec.class).orElseThrow();
        assertEquals(20.0D, spec.base());
        assertEquals(0.0D, spec.min());
        assertEquals(40.0D, spec.max());
        assertEquals(StatFormat.INTEGER, spec.format());

        Stat stat = spec.toStat(ContentId.parse("test:health"));
        assertEquals(40.0D, stat.clamp(100.0D));
    }

    @Test
    public void defaultsWhenOmitted(@TempDir Path root) throws Exception {
        write(root, "power.yml", "id: test:power", "base: 5.0");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        StatSpec spec = result.get(ContentId.parse("test:power"), StatSpec.class).orElseThrow();
        assertEquals(5.0D, spec.base());
        assertEquals(StatFormat.DECIMAL, spec.format());
    }

    @Test
    public void badRangeAndFormatCollected(@TempDir Path root) throws Exception {
        write(root, "bad-range.yml", "id: test:badrange", "base: 5.0", "min: 10.0", "max: 0.0");
        write(root, "bad-format.yml", "id: test:badformat", "base: 1.0", "format: SHINY");

        SpecLoadResult result = loader().load(root);
        assertFalse(result.successful());
        assertEquals(2, result.errors().size(), () -> result.errors().toString());
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("greater than max")));
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("unknown stat format 'SHINY'")));
    }
}
