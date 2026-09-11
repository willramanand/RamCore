package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour every {@link Store} backend must satisfy. Backends extend this and implement the
 * factory hooks; async backends get their work driven by {@link #settle()}.
 */
public abstract class StoreContractTest {
    protected FakeScheduler scheduler;
    protected Store<String, Profile> store;

    /** Creates a fresh, empty store with the given migrations. */
    protected abstract Store<String, Profile> newStore(StoreMigrations<Profile> migrations);

    /** Writes a record at an explicit version, bypassing the store API (for migration tests). */
    protected abstract void seed(Store<String, Profile> store, String key, int version, Profile value);

    /** The version currently persisted for a key, bypassing the store API. */
    protected abstract Optional<Integer> persistedVersion(Store<String, Profile> store, String key);

    @BeforeEach
    void setUpContract() {
        this.scheduler = FakeScheduler.install();
        this.store = newStore(StoreMigrations.none());
    }

    @AfterEach
    void tearDownContract() throws Exception {
        this.store.close();
        this.scheduler.close();
    }

    /** Drives async work to completion. Sync backends have nothing to do. */
    protected void settle() {
        this.scheduler.runAll();
    }

    protected <T> T await(Promise<T> promise) {
        settle();
        assertTrue(promise.isDone(), "promise did not complete after settling the scheduler");
        return promise.join();
    }

    @Test
    public void loadMissingIsEmpty() {
        assertEquals(Optional.empty(), await(this.store.load("missing")));
    }

    @Test
    public void saveThenLoadRoundTrips() {
        Profile profile = new Profile("will", 12);

        await(this.store.save("will", profile));

        assertEquals(Optional.of(profile), await(this.store.load("will")));
    }

    @Test
    public void saveOverwrites() {
        await(this.store.save("will", new Profile("will", 1)));
        await(this.store.save("will", new Profile("will", 2)));

        assertEquals(2, await(this.store.load("will")).orElseThrow().level());
    }

    @Test
    public void deleteRemovesAndReportsPresence() {
        await(this.store.save("will", new Profile("will", 1)));

        assertTrue(await(this.store.delete("will")));
        assertFalse(await(this.store.delete("will")));
        assertEquals(Optional.empty(), await(this.store.load("will")));
    }

    @Test
    public void keysAndLoadAllCoverEveryEntry() {
        await(this.store.save("a", new Profile("a", 1)));
        await(this.store.save("b", new Profile("b", 2)));

        assertEquals(Set.of("a", "b"), await(this.store.keys()));
        Map<String, Profile> all = await(this.store.loadAll());
        assertEquals(2, all.size());
        assertEquals(new Profile("b", 2), all.get("b"));
    }

    @Test
    public void migrationsApplyOnLoadAndWriteBack() throws Exception {
        this.store.close();
        this.store = newStore(StoreMigrations.<Profile>start()
                .to(2, (Profile profile, int from) -> new Profile(profile.name(), profile.level() + 100))
                .to(3, (Profile profile, int from) -> new Profile(profile.name().toUpperCase(), profile.level())));
        seed(this.store, "old", 1, new Profile("will", 1));
        seed(this.store, "mid", 2, new Profile("mid", 5));

        assertEquals(new Profile("WILL", 101), await(this.store.load("old")).orElseThrow());
        assertEquals(new Profile("MID", 5), await(this.store.load("mid")).orElseThrow());
        assertEquals(Optional.of(3), persistedVersion(this.store, "old"), "migrated record is written back at the current version");
        assertEquals(Optional.of(3), persistedVersion(this.store, "mid"));

        await(this.store.save("new", new Profile("new", 1)));
        assertEquals(Optional.of(3), persistedVersion(this.store, "new"), "new records are written at the current version");
    }

    public record Profile(String name, int level) {
    }
}
