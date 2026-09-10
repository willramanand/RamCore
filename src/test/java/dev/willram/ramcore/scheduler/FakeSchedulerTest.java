package dev.willram.ramcore.scheduler;

import dev.willram.ramcore.exception.types.EntityRetiredException;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FakeSchedulerTest {
    private FakeScheduler scheduler;

    @BeforeEach
    void installScheduler() {
        this.scheduler = FakeScheduler.install();
        assertSame(this.scheduler, SchedulerBackends.current());
    }

    @AfterEach
    void restoreScheduler() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    @Test
    public void globalWorkFromSyncThreadRunsInline() {
        AtomicBoolean ran = new AtomicBoolean(false);

        Promise<Void> promise = Schedulers.runGlobal(() -> ran.set(true));

        assertTrue(ran.get());
        assertTrue(promise.isDone());
        // RamPromise short-circuits sync work on the sync thread without touching the backend
        assertEquals(0, this.scheduler.pendingSync());
    }

    @Test
    public void rawSyncExecuteFromSyncThreadRunsInlineThroughBackend() {
        AtomicBoolean ran = new AtomicBoolean(false);

        Schedulers.sync().execute(() -> ran.set(true));

        assertTrue(ran.get());
        assertEquals(List.of("global"), this.scheduler.executed());
    }

    @Test
    public void asyncWorkWaitsForDrain() {
        AtomicBoolean ran = new AtomicBoolean(false);

        Promise<Void> promise = Schedulers.runAsync(() -> ran.set(true));

        assertFalse(ran.get());
        assertFalse(promise.isDone());
        assertEquals(1, this.scheduler.runAsync());
        assertTrue(ran.get());
        assertTrue(promise.isDone());
        assertFalse(Schedulers.isSyncThread() == false, "sync thread status restored after async drain");
    }

    @Test
    public void timerRunsOnScheduleAndStops() {
        AtomicInteger runs = new AtomicInteger();

        Task task = Schedulers.runTimer(TaskContext.global(), runs::incrementAndGet, 1L, 2L);

        this.scheduler.tick();
        assertEquals(1, runs.get());
        this.scheduler.tick(2);
        assertEquals(2, runs.get());
        this.scheduler.tick(2);
        assertEquals(3, runs.get());
        assertEquals(3, task.getTimesRan());

        task.stop();
        this.scheduler.tick(4);
        assertEquals(3, runs.get());
        assertTrue(task.isClosed());
    }

    @Test
    public void regionWorkWaitsOneTickAndRecordsContext() {
        AtomicBoolean ran = new AtomicBoolean(false);
        Location location = new Location(null, 1.5, 2.0, -3.5);

        Promise<Void> promise = Schedulers.run(location, () -> ran.set(true));

        assertFalse(ran.get());
        this.scheduler.tick();
        assertTrue(ran.get());
        assertTrue(promise.isDone());
        assertEquals(List.of("region:?@1,2,-4"), this.scheduler.executed());
    }

    @Test
    public void retiredEntityRunsRetiredCallbackAndCancelsPromise() {
        UUID id = UUID.randomUUID();
        Entity entity = ProxyFakes.proxy(Entity.class, Map.of("getUniqueId", id));
        AtomicBoolean ran = new AtomicBoolean(false);
        AtomicBoolean retired = new AtomicBoolean(false);

        this.scheduler.retireEntity(id);
        Promise<Void> promise = Schedulers.run(entity, () -> ran.set(true), () -> retired.set(true));

        assertTrue(retired.get());
        assertFalse(ran.get());
        assertTrue(promise.isDone());
        assertInstanceOf(EntityRetiredException.class, assertThrows(CompletionException.class, promise::join).getCause());
    }

    @Test
    public void retiringEntityDropsQueuedWork() {
        UUID id = UUID.randomUUID();
        Entity entity = ProxyFakes.proxy(Entity.class, Map.of("getUniqueId", id));
        AtomicBoolean ran = new AtomicBoolean(false);

        Promise<Void> promise = Schedulers.run(entity, () -> ran.set(true));
        assertEquals(1, this.scheduler.pendingScheduled());

        this.scheduler.retireEntity(id);
        this.scheduler.tick();

        assertFalse(ran.get());
        assertTrue(promise.isDone(), "entity-bound promise completes when the entity is retired");
        assertInstanceOf(EntityRetiredException.class, assertThrows(CompletionException.class, promise::join).getCause());
    }

    @Test
    public void throwingTaskIsRecordedAndDoesNotStopTheScheduler() {
        AtomicBoolean after = new AtomicBoolean(false);

        // raw executor path: the backend sees the throwable
        Schedulers.async().execute(() -> {
            throw new IllegalStateException("boom");
        });
        // promise path: the promise captures it and completes exceptionally
        Promise<Void> failed = Schedulers.runAsync(() -> {
            throw new IllegalStateException("promise boom");
        });
        Schedulers.runAsync(() -> after.set(true));
        this.scheduler.runAsync();

        assertTrue(after.get());
        assertEquals(1, this.scheduler.errors().size());
        assertEquals("boom", this.scheduler.errors().getFirst().getMessage());
        assertTrue(failed.isDone());
        assertTrue(failed.toCompletableFuture().isCompletedExceptionally());
    }
}
