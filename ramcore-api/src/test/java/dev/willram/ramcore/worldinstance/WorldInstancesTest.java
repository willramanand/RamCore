package dev.willram.ramcore.worldinstance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WorldInstancesTest {

    @Test
    public void copyTemplateSkipsSessionFiles(@TempDir Path root) throws IOException {
        Path template = root.resolve("template");
        Files.createDirectories(template.resolve("region"));
        Files.writeString(template.resolve("level.dat"), "level");
        Files.writeString(template.resolve("uid.dat"), "uid");
        Files.writeString(template.resolve("session.lock"), "lock");
        Files.writeString(template.resolve("region").resolve("r.0.0.mca"), "chunk");

        Path target = root.resolve("ramcore_inst_dungeon_abc");
        WorldInstances.copyTemplate(template, target);

        assertTrue(Files.exists(target.resolve("level.dat")));
        assertTrue(Files.exists(target.resolve("region").resolve("r.0.0.mca")));
        assertFalse(Files.exists(target.resolve("uid.dat")));
        assertFalse(Files.exists(target.resolve("session.lock")));
    }

    @Test
    public void missingTemplateThrows(@TempDir Path root) {
        assertThrows(IOException.class,
                () -> WorldInstances.copyTemplate(root.resolve("nope"), root.resolve("t")));
    }

    @Test
    public void markerRoundTrip(@TempDir Path root) throws IOException {
        Path dir = root.resolve("ramcore_inst_dungeon_abc");
        new InstanceMarker("dungeon", "abc", 12345L).writeTo(dir);

        assertTrue(InstanceMarker.present(dir));
        InstanceMarker read = InstanceMarker.readFrom(dir).orElseThrow();
        assertEquals("dungeon", read.template());
        assertEquals("abc", read.id());
        assertEquals(12345L, read.createdAt());
    }

    @Test
    public void sweepFindsOnlyMarkedInstanceDirs(@TempDir Path container) throws IOException {
        Path inst = container.resolve("ramcore_inst_dungeon_abc");
        new InstanceMarker("dungeon", "abc", 1L).writeTo(inst);
        Files.createDirectories(container.resolve("ramcore_inst_nomarker_xyz")); // prefix but no marker
        Files.createDirectories(container.resolve("world"));                      // a normal world

        List<Path> found = WorldInstances.sweep(container);
        assertEquals(1, found.size());
        assertEquals(inst.getFileName(), found.get(0).getFileName());
    }

    @Test
    public void deleteRecursivelyRemovesTree(@TempDir Path root) throws IOException {
        Path dir = root.resolve("ramcore_inst_dungeon_abc");
        new InstanceMarker("dungeon", "abc", 1L).writeTo(dir);
        Files.createDirectories(dir.resolve("region"));
        Files.writeString(dir.resolve("region").resolve("r.mca"), "x");

        WorldInstances.deleteRecursively(dir);
        assertFalse(Files.exists(dir));
    }
}
