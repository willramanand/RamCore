package dev.willram.ramcore.data;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FileDataRepositoryTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savesAndLoadsJsonItemsByStringKey() throws Exception {
        Path directory = temporaryFolder.newFolder("repo").toPath();
        FileDataRepository<String, Profile> repository = repository(directory);
        Profile profile = new Profile("will", 12);

        repository.add("players/will", profile);
        profile.markDirty();
        repository.saveDirty();

        FileDataRepository<String, Profile> loaded = repository(directory);
        loaded.setup();

        assertTrue(Files.exists(repository.path("players/will")));
        assertEquals("will", loaded.require("players/will").name);
        assertEquals(12, loaded.require("players/will").level);
        assertFalse(loaded.require("players/will").dirty());
    }

    @Test
    public void queueSavePersistsDirtyItemAndFlushesOnClose() throws Exception {
        Path directory = temporaryFolder.newFolder("queued").toPath();
        FileDataRepository<String, Profile> repository = repository(directory);
        Profile profile = new Profile("queued", 3);
        repository.add("queued", profile);
        profile.markDirty();

        CompletableFuture<Void> queued = repository.queueSave("queued");
        repository.close();

        assertTrue(queued.isDone());
        assertFalse(profile.isSaving());
        assertFalse(profile.dirty());
        assertTrue(Files.exists(repository.path("queued")));
    }

    @Test
    public void migrationsUpgradeVersionAndMarkItemDirty() throws Exception {
        Path directory = temporaryFolder.newFolder("migrations").toPath();
        FileDataRepository<String, Profile> repository = repository(directory);
        Profile profile = new Profile("legacy", 1);
        profile.dataVersion(1);
        repository.add("legacy", profile);
        repository.saveAll();

        FileDataRepository<String, Profile> migrated = repository(directory)
                .migrateTo(2, (item, fromVersion) -> {
                    item.level = item.level + 10;
                    return item;
                });
        migrated.setup();

        Profile loaded = migrated.require("legacy");
        assertEquals(2, loaded.dataVersion());
        assertEquals(11, loaded.level);
        assertTrue(loaded.dirty());
    }

    @Test
    public void deleteRemovesMemoryAndFileState() throws Exception {
        Path directory = temporaryFolder.newFolder("delete").toPath();
        FileDataRepository<String, Profile> repository = repository(directory);
        repository.add("remove-me", new Profile("remove-me", 1));
        repository.saveAll();

        repository.delete("remove-me");

        assertFalse(repository.has("remove-me"));
        assertFalse(Files.exists(repository.path("remove-me")));
    }

    @Test
    public void keyCodecRoundTripsUnsafeStringKeys() {
        DataKeyCodec<String> codec = DataKeyCodec.stringKeys();
        String key = "players/will ram";

        String encoded = codec.encode(key);

        assertFalse(encoded.contains("/"));
        assertEquals(key, codec.decode(encoded));
    }

    @Test
    public void queueSaveDirtyOnlyQueuesDirtyItems() throws Exception {
        Path directory = temporaryFolder.newFolder("dirty").toPath();
        FileDataRepository<String, Profile> repository = repository(directory);
        Profile dirty = new Profile("dirty", 1);
        Profile clean = new Profile("clean", 1);
        dirty.markDirty();
        repository.add("dirty", dirty);
        repository.add("clean", clean);

        List<CompletableFuture<Void>> futures = repository.queueSaveDirty();
        repository.flushQueuedSaves();

        assertEquals(1, futures.size());
        assertTrue(Files.exists(repository.path("dirty")));
        assertFalse(Files.exists(repository.path("clean")));
    }

    private static FileDataRepository<String, Profile> repository(Path directory) {
        return Repositories.jsonByString(directory, Profile.class, Runnable::run);
    }

    public static final class Profile extends DataItem {
        private String name;
        private int level;

        public Profile() {
        }

        private Profile(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }
}
