package dev.willram.ramcore.store;

import dev.willram.ramcore.data.DataItem;
import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.data.FileDataRepository;
import dev.willram.ramcore.data.GsonDataSerializer;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Files written by the deprecated {@link FileDataRepository} must be readable by a {@link FileStore}
 * using the {@link StoreCodec#dataItem(Class)} codec, and vice versa.
 */
@SuppressWarnings("deprecation")
public final class FileStoreCompatibilityTest {
    @TempDir
    Path tempDir;

    private FakeScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
    }

    @Test
    public void fileStoreReadsLegacyRepositoryFiles() {
        Path directory = this.tempDir.resolve("legacy");
        FileDataRepository<String, Legacy> repository = new FileDataRepository<>(
                directory, DataKeyCodec.stringKeys(), GsonDataSerializer.pretty(Legacy.class), Runnable::run);
        repository.setup();
        Legacy item = new Legacy("will", 5);
        item.dataVersion(2);
        repository.add("will", item);
        repository.saveAll();
        repository.close();

        FileStore<String, Legacy> store = Stores.file(directory, DataKeyCodec.stringKeys(), StoreCodec.dataItem(Legacy.class),
                StoreMigrations.<Legacy>start().to(2, (value, from) -> value).to(3, (value, from) -> {
                    value.level += 100;
                    return value;
                }));
        Promise<Optional<Legacy>> loaded = store.load("will");
        this.scheduler.runAll();

        Legacy migrated = loaded.join().orElseThrow();
        assertEquals("will", migrated.name);
        assertEquals(105, migrated.level, "only the version 3 step applied to a version 2 file");
        assertEquals(3, migrated.dataVersion());
        Promise<Set<String>> keys = store.keys();
        this.scheduler.runAll();
        assertEquals(Set.of("will"), keys.join());
    }

    @Test
    public void legacyRepositoryReadsFileStoreFiles() {
        Path directory = this.tempDir.resolve("modern");
        FileStore<String, Legacy> store = Stores.file(directory, DataKeyCodec.stringKeys(), StoreCodec.dataItem(Legacy.class));
        store.save("will", new Legacy("will", 9));
        this.scheduler.runAll();

        FileDataRepository<String, Legacy> repository = new FileDataRepository<>(
                directory, DataKeyCodec.stringKeys(), GsonDataSerializer.pretty(Legacy.class), Runnable::run);
        repository.setup();

        assertTrue(repository.has("will"));
        assertEquals(9, repository.require("will").level);
        assertEquals(1, repository.require("will").dataVersion());
    }

    public static final class Legacy extends DataItem {
        String name;
        int level;

        public Legacy() {
        }

        Legacy(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }
}
