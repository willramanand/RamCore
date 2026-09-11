package dev.willram.ramcore.worldinstance;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WorldInstanceServiceTest {

    private FakeScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    private static final class FakeBackend implements WorldBackend {
        private final Path container;
        private final boolean supported;
        final List<String> loaded = new ArrayList<>();
        final List<String> unloaded = new ArrayList<>();
        final List<String> evacuated = new ArrayList<>();

        FakeBackend(Path container, boolean supported) {
            this.container = container;
            this.supported = supported;
        }

        public boolean supportsInstances() {
            return this.supported;
        }

        public Path worldContainer() {
            return this.container;
        }

        public Promise<Void> loadWorld(String name) {
            this.loaded.add(name);
            return Promise.completed(null);
        }

        public Promise<Void> unloadWorld(String name, boolean save) {
            this.unloaded.add(name);
            return Promise.completed(null);
        }

        public Promise<Void> evacuate(String name) {
            this.evacuated.add(name);
            return Promise.completed(null);
        }
    }

    private static Path template(Path root) throws Exception {
        Path templates = root.resolve("world-templates");
        Path dungeon = templates.resolve("dungeon");
        Files.createDirectories(dungeon.resolve("region"));
        Files.writeString(dungeon.resolve("level.dat"), "level");
        Files.writeString(dungeon.resolve("region").resolve("r.0.0.mca"), "chunk");
        return templates;
    }

    @Test
    public void createCopiesMarksAndLoads(@TempDir Path root) throws Exception {
        Path templates = template(root);
        Path container = Files.createDirectories(root.resolve("container"));
        FakeBackend backend = new FakeBackend(container, true);
        WorldInstanceService service = new WorldInstanceService(backend, templates);

        Promise<WorldInstance> promise = service.create("dungeon", WorldInstanceOptions.defaults());
        this.scheduler.runAll();
        WorldInstance instance = promise.join();

        assertTrue(instance.name().startsWith("ramcore_inst_dungeon_"));
        assertTrue(Files.exists(instance.directory().resolve("level.dat")));
        assertTrue(InstanceMarker.present(instance.directory()));
        assertTrue(backend.loaded.contains(instance.name()));
    }

    @Test
    public void createFailsWhenUnsupported(@TempDir Path root) throws Exception {
        Path templates = template(root);
        FakeBackend backend = new FakeBackend(Files.createDirectories(root.resolve("c")), false);
        WorldInstanceService service = new WorldInstanceService(backend, templates);

        Promise<WorldInstance> promise = service.create("dungeon", WorldInstanceOptions.defaults());
        this.scheduler.runAll();
        assertThrows(RuntimeException.class, promise::join);
    }

    @Test
    public void closeEvacuatesUnloadsAndDeletes(@TempDir Path root) throws Exception {
        Path templates = template(root);
        Path container = Files.createDirectories(root.resolve("container"));
        FakeBackend backend = new FakeBackend(container, true);
        WorldInstanceService service = new WorldInstanceService(backend, templates);

        Promise<WorldInstance> promise = service.create("dungeon", WorldInstanceOptions.defaults());
        this.scheduler.runAll();
        WorldInstance instance = promise.join();
        assertTrue(Files.exists(instance.directory()));

        Promise<Void> closed = instance.closeAsync();
        this.scheduler.runAll();
        closed.join();

        assertTrue(backend.evacuated.contains(instance.name()));
        assertTrue(backend.unloaded.contains(instance.name()));
        assertFalse(Files.exists(instance.directory()));
    }

    @Test
    public void sweepStartupDeletesLeftovers(@TempDir Path root) throws Exception {
        Path templates = template(root);
        Path container = Files.createDirectories(root.resolve("container"));
        Path leftover = container.resolve("ramcore_inst_dungeon_old");
        new InstanceMarker("dungeon", "old", 1L).writeTo(leftover);

        FakeBackend backend = new FakeBackend(container, true);
        WorldInstanceService service = new WorldInstanceService(backend, templates);

        assertEquals(1, service.sweepStartup());
        assertFalse(Files.exists(leftover));
    }
}
