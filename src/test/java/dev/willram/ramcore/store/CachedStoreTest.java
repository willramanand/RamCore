package dev.willram.ramcore.store;

import dev.willram.ramcore.exception.ApiMisuseException;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.store.StoreContractTest.Profile;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CachedStoreTest {
    private FakeScheduler scheduler;
    private RecordingAsyncStore<String, Profile> backing;
    private CachedStore<String, Profile> cache;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.backing = new RecordingAsyncStore<>(StoreMigrations.none());
        this.cache = Stores.cached(this.backing);
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
    }

    @Test
    public void putMarksDirtyWithoutIo() {
        this.cache.put("will", new Profile("will", 1));

        assertTrue(this.cache.dirty("will"));
        assertEquals(Set.of("will"), this.cache.dirtyKeys());
        assertEquals(Optional.of(new Profile("will", 1)), this.cache.cached("will"));
        assertTrue(this.backing.calls.isEmpty());
        assertEquals(0, this.scheduler.pendingAsync());
    }

    @Test
    public void saveDirtyWritesOnlyDirtyEntriesAndClearsMarks() {
        this.cache.put("a", new Profile("a", 1));
        this.cache.put("b", new Profile("b", 1));
        this.cache.clearDirty("b");

        Promise<Integer> flushed = this.cache.saveDirty();
        this.scheduler.runAll();

        assertEquals(1, flushed.join());
        assertEquals(List.of("save:a=Profile[name=a, level=1]"), this.backing.calls);
        assertTrue(this.cache.dirtyKeys().isEmpty());
        assertEquals(0, this.cache.saveDirty().join(), "nothing dirty means nothing written");
    }

    @Test
    public void saveWritesThroughAndIsNotDirty() {
        Promise<Void> saved = this.cache.save("will", new Profile("will", 1));
        this.scheduler.runAll();

        assertTrue(saved.isDone());
        assertFalse(this.cache.dirty("will"));
        assertEquals(1, this.backing.entries.size());
        assertEquals(new Profile("will", 1), this.cache.require("will"));
    }

    @Test
    public void loadCachesAndSecondLoadSkipsBackend() {
        this.backing.entries.put("will", StoredRecord.of(new Profile("will", 7)));

        Promise<Optional<Profile>> first = this.cache.load("will");
        this.scheduler.runAll();
        assertEquals(7, first.join().orElseThrow().level());

        Promise<Optional<Profile>> second = this.cache.load("will");
        assertTrue(second.isDone(), "cached value answers synchronously");
        assertEquals(List.of("load:will"), this.backing.calls, "backend hit exactly once");
    }

    @Test
    public void requireFailsFastWhenNotCached() {
        ApiMisuseException error = assertThrows(ApiMisuseException.class, () -> this.cache.require("nobody"));

        assertTrue(error.getMessage().contains("load(key)"));
    }

    @Test
    public void markDirtyRequiresCachedValue() {
        assertThrows(ApiMisuseException.class, () -> this.cache.markDirty("nobody"));
    }

    @Test
    public void deleteEvictsAndRemovesFromBackend() {
        this.cache.put("will", new Profile("will", 1));
        this.cache.saveDirty();
        this.scheduler.runAll();

        Promise<Boolean> deleted = this.cache.delete("will");
        this.scheduler.runAll();

        assertTrue(deleted.join());
        assertTrue(this.cache.cached("will").isEmpty());
        assertFalse(this.cache.dirty("will"));
        assertTrue(this.backing.entries.isEmpty());
    }

    @Test
    public void loadAllPopulatesCacheWithoutOverwritingDirtyEdits() {
        this.backing.entries.put("a", StoredRecord.of(new Profile("a", 1)));
        this.backing.entries.put("b", StoredRecord.of(new Profile("b", 1)));
        this.cache.put("b", new Profile("b", 99));

        Promise<java.util.Map<String, Profile>> all = this.cache.loadAll();
        this.scheduler.runAll();

        assertEquals(1, all.join().get("a").level());
        assertEquals(99, all.join().get("b").level(), "unsaved edit wins over the backend copy");
        assertTrue(this.cache.dirty("b"));
    }

    @Test
    public void closeFlushesDirtyEntriesWhenBackendIsSynchronous() throws Exception {
        InMemoryStore<String, Profile> memory = Stores.inMemory();
        CachedStore<String, Profile> cached = Stores.cached(memory);
        cached.put("will", new Profile("will", 1));

        cached.close();

        assertTrue(cached.isClosed());
        assertEquals(1, memory.size());
        assertTrue(cached.dirtyKeys().isEmpty());
    }

    @Test
    public void putDuringFlushRedirtiesTheKey() {
        this.cache.put("will", new Profile("will", 1));
        this.backing.beforeSave = key -> this.cache.put("will", new Profile("will", 2));

        this.cache.saveDirty();
        this.scheduler.runAll();

        assertTrue(this.cache.dirty("will"), "an edit that raced the write must be saved next time");
        assertEquals(2, this.cache.require("will").level());
    }
}
