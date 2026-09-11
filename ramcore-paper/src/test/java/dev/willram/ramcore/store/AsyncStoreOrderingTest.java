package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.store.StoreContractTest.Profile;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AsyncStoreOrderingTest {
    private FakeScheduler scheduler;
    private RecordingAsyncStore<String, Profile> store;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.store = new RecordingAsyncStore<>(StoreMigrations.none());
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
    }

    @Test
    public void workRunsOnTheAsyncSchedulerNotTheCaller() {
        Promise<Void> save = this.store.save("will", new Profile("will", 1));

        assertFalse(save.isDone());
        assertTrue(this.store.calls.isEmpty());
        assertEquals(1, this.scheduler.pendingAsync());

        this.scheduler.runAsync();
        assertTrue(save.isDone());
        assertEquals(List.of("save:will=Profile[name=will, level=1]"), this.store.calls);
    }

    @Test
    public void operationsOnTheSameKeyRunInSubmissionOrder() {
        Promise<Void> first = this.store.save("will", new Profile("will", 1));
        Promise<Void> second = this.store.save("will", new Profile("will", 2));
        Promise<Optional<Profile>> load = this.store.load("will");

        // only the head of the lane is queued; the rest wait for it
        assertEquals(1, this.scheduler.pendingAsync());

        this.scheduler.runAll();

        assertTrue(first.isDone() && second.isDone() && load.isDone());
        assertEquals(List.of(
                "save:will=Profile[name=will, level=1]",
                "save:will=Profile[name=will, level=2]",
                "load:will"
        ), this.store.calls);
        assertEquals(2, load.join().orElseThrow().level());
    }

    @Test
    public void operationsOnDifferentKeysAreAllQueuedImmediately() {
        this.store.save("a", new Profile("a", 1));
        this.store.save("b", new Profile("b", 1));

        assertEquals(2, this.scheduler.pendingAsync(), "independent keys do not wait on each other");
        this.scheduler.runAll();
        assertEquals(2, this.store.entries.size());
    }

    @Test
    public void failureCompletesThePromiseExceptionallyAndReleasesTheLane() {
        java.util.concurrent.atomic.AtomicBoolean failOnce = new java.util.concurrent.atomic.AtomicBoolean(true);
        this.store.beforeSave = key -> {
            if (failOnce.getAndSet(false)) {
                throw new IllegalStateException("disk full");
            }
        };
        Promise<Void> failed = this.store.save("bad", new Profile("bad", 1));
        Promise<Void> next = this.store.save("bad", new Profile("bad", 2));

        this.scheduler.runAll();

        CompletionException error = assertThrows(CompletionException.class, failed::join);
        assertInstanceOf(StoreException.class, error.getCause());
        assertInstanceOf(IllegalStateException.class, error.getCause().getCause());
        assertTrue(next.isDone(), "a failed operation must not block later operations on the lane");
        assertEquals(2, this.store.entries.get("bad").value().level());
    }

    @Test
    public void migrationWritesBackInsideTheSameOperation() {
        RecordingAsyncStore<String, Profile> migrating = new RecordingAsyncStore<>(
                StoreMigrations.<Profile>start().to(2, (Profile p, int from) -> new Profile(p.name(), p.level() * 10)));
        migrating.entries.put("old", StoredRecord.of(1, new Profile("old", 3)));

        Promise<Optional<Profile>> load = migrating.load("old");
        this.scheduler.runAll();

        assertEquals(30, load.join().orElseThrow().level());
        assertEquals(List.of("load:old", "save:old=Profile[name=old, level=30]"), migrating.calls);
        assertEquals(2, migrating.entries.get("old").dataVersion());
    }

    @Test
    public void loadAllUsesItsOwnLane() {
        this.store.entries.put("a", StoredRecord.of(new Profile("a", 1)));

        Promise<java.util.Map<String, Profile>> all = this.store.loadAll();
        Promise<Void> save = this.store.save("b", new Profile("b", 1));

        assertEquals(2, this.scheduler.pendingAsync());
        this.scheduler.runAll();
        assertTrue(all.isDone() && save.isDone());
    }
}
