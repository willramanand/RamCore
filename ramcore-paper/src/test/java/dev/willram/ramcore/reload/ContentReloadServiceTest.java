package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ContentReloadServiceTest {
    private static final ContentId A = ContentId.parse("test:a");

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

    private static final class FakeBound implements TemplateBound {
        private final ContentId id;
        private Object resolved;
        private int rebinds;

        FakeBound(ContentId id) {
            this.id = id;
        }

        @Override
        public ContentId templateId() {
            return this.id;
        }

        @Override
        public void rebind(Object resolved) {
            this.resolved = resolved;
            this.rebinds++;
        }
    }

    private static void write(Path root, String name, String... lines) throws Exception {
        Path dir = root.resolve("widgets");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    public void firstReloadRebindsLiveObjects(@TempDir Path root) throws Exception {
        write(root, "a.yml", "id: test:a", "x: 1");
        LiveObjectRegistry registry = new LiveObjectRegistry();
        FakeBound live = new FakeBound(A);
        registry.register(live);

        ContentReloadService service = new ContentReloadService(registry);
        service.register(new ContentPack("test", root, def -> "RESOLVED:" + def.id()));

        var p = service.reload("test");
        this.scheduler.runAll();
        ContentDiff diff = p.join();

        assertTrue(diff.added().contains(A), () -> diff.added().toString());
        assertTrue(diff.rebuilt().contains(A));
        assertEquals("RESOLVED:test:a", live.resolved);
        assertEquals(1, live.rebinds);
    }

    @Test
    public void unchangedReloadDoesNotRebind(@TempDir Path root) throws Exception {
        write(root, "a.yml", "id: test:a", "x: 1");
        LiveObjectRegistry registry = new LiveObjectRegistry();
        FakeBound live = new FakeBound(A);
        registry.register(live);
        ContentReloadService service = new ContentReloadService(registry);
        service.register(new ContentPack("test", root, def -> "R"));

        { var p0 = service.reload("test"); this.scheduler.runAll(); p0.join(); }
        assertEquals(1, live.rebinds);

        // reload with no file change
        var p = service.reload("test");
        this.scheduler.runAll();
        ContentDiff diff = p.join();
        assertFalse(diff.dirty());
        assertTrue(diff.rebuilt().isEmpty());
        assertEquals(1, live.rebinds);
    }

    @Test
    public void changedReloadRebinds(@TempDir Path root) throws Exception {
        write(root, "a.yml", "id: test:a", "x: 1");
        LiveObjectRegistry registry = new LiveObjectRegistry();
        FakeBound live = new FakeBound(A);
        registry.register(live);
        ContentReloadService service = new ContentReloadService(registry);
        service.register(new ContentPack("test", root, def -> "R" + def.node().node("x").getInt()));

        { var p0 = service.reload("test"); this.scheduler.runAll(); p0.join(); }

        write(root, "a.yml", "id: test:a", "x: 2");
        var p = service.reload("test");
        this.scheduler.runAll();
        ContentDiff diff = p.join();

        assertTrue(diff.changed().contains(A));
        assertTrue(diff.rebuilt().contains(A));
        assertEquals("R2", live.resolved);
        assertEquals(2, live.rebinds);
    }

    @Test
    public void resolveFailureCollected(@TempDir Path root) throws Exception {
        write(root, "a.yml", "id: test:a", "x: 1");
        LiveObjectRegistry registry = new LiveObjectRegistry();
        registry.register(new FakeBound(A));
        ContentReloadService service = new ContentReloadService(registry);
        service.register(new ContentPack("test", root, def -> {
            throw new IllegalStateException("boom");
        }));

        var p = service.reload("test");
        this.scheduler.runAll();
        ContentDiff diff = p.join();

        assertTrue(diff.failed().contains(A));
        assertFalse(diff.rebuilt().contains(A));
    }
}
