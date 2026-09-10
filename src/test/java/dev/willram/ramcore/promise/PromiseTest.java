package dev.willram.ramcore.promise;

import dev.willram.ramcore.exception.types.EntityRetiredException;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PromiseTest {
    private FakeScheduler scheduler;

    @BeforeEach
    void installScheduler() {
        this.scheduler = FakeScheduler.install();
    }

    @AfterEach
    void restoreScheduler() {
        this.scheduler.close();
    }

    @Test
    public void asyncSupplyThenSyncContinuationRunsInOrder() {
        List<String> log = new ArrayList<>();

        Promise<Integer> promise = Promise.supplyingAsync(() -> {
            log.add("supply");
            return 2;
        }).thenApplySync(value -> {
            log.add("apply");
            return value * 21;
        });

        assertFalse(promise.isDone());
        assertEquals(1, this.scheduler.pendingAsync());

        this.scheduler.runAsync();
        assertEquals(List.of("supply"), log);
        assertEquals(1, this.scheduler.pendingSync(), "sync continuation queues when completed from async");

        this.scheduler.tick();
        assertEquals(List.of("supply", "apply"), log);
        assertEquals(42, promise.join());
        assertEquals(List.of("async", "global"), this.scheduler.executed());
    }

    @Test
    public void syncContinuationOnSyncThreadRunsInline() {
        Promise<Integer> promise = Promise.completed(1).thenApplySync(value -> value + 1);

        assertTrue(promise.isDone());
        assertEquals(2, promise.join());
        assertEquals(0, this.scheduler.pendingSync());
    }

    @Test
    public void supplierExceptionReachesExceptionallyHandler() {
        Promise<String> promise = Promise.<String>supplyingExceptionallyAsync(() -> {
            throw new IllegalStateException("boom");
        }).exceptionallySync(error -> "recovered");

        this.scheduler.runAll();

        assertTrue(promise.isDone());
        assertEquals("recovered", promise.join());
    }

    @Test
    public void cancelBeforeSupplySkipsSupplier() {
        AtomicBoolean ran = new AtomicBoolean(false);
        Promise<Integer> promise = Promise.supplyingAsync(() -> {
            ran.set(true);
            return 1;
        });

        promise.cancel();
        this.scheduler.runAsync();

        assertTrue(promise.isCancelled());
        assertFalse(ran.get());
    }

    @Test
    public void delayedSyncSupplyCompletesOnDueTick() {
        Promise<String> promise = Promise.supplyingDelayedSync(() -> "late", 5L);

        this.scheduler.tick(4);
        assertFalse(promise.isDone());

        this.scheduler.tick();
        assertTrue(promise.isDone());
        assertEquals("late", promise.join());
    }

    @Test
    public void cancelAfterCompletionDoesNotPoisonLaterContinuations() {
        Promise<Integer> promise = Promise.completed(1);

        assertFalse(promise.cancel(), "cancelling a completed promise is a no-op");
        Promise<Integer> derived = promise.thenApplySync(value -> value + 1);

        assertTrue(derived.isDone());
        assertEquals(2, derived.join());
    }

    @Test
    public void cancellingDerivedPromiseSkipsItsContinuation() {
        AtomicBoolean applied = new AtomicBoolean(false);
        Promise<Integer> upstream = Promise.empty();
        Promise<Integer> derived = upstream.thenApplySync(value -> {
            applied.set(true);
            return value;
        });

        derived.cancel();
        upstream.supply(1);
        this.scheduler.runAll();

        assertTrue(derived.isCancelled());
        assertFalse(applied.get());
        assertEquals(1, upstream.join(), "upstream is unaffected by a derived cancel");
    }

    @Test
    public void exceptionallyHandlerRunsWhenUpstreamIsCancelled() {
        Promise<String> upstream = Promise.empty();
        Promise<String> recovered = upstream.exceptionallySync(error -> "fallback");

        upstream.cancel();
        this.scheduler.runAll();

        assertTrue(recovered.isDone(), "derived promise must not hang after an upstream cancel");
        assertEquals("fallback", recovered.join());
    }

    @Test
    public void entityContinuationRunsOnTheEntityQueue() {
        UUID id = UUID.randomUUID();
        Entity entity = ProxyFakes.proxy(Entity.class, Map.of("getUniqueId", id));

        Promise<Integer> promise = Promise.completed(1).thenApply(TaskContext.of(entity), value -> value + 1);

        assertFalse(promise.isDone(), "entity work never runs inline");
        this.scheduler.tick();
        assertEquals(2, promise.join());
        assertEquals(List.of("entity:" + id), this.scheduler.executed());
    }

    @Test
    public void entityContinuationFailsWithEntityRetiredWhenEntityIsRemoved() {
        UUID id = UUID.randomUUID();
        Entity entity = ProxyFakes.proxy(Entity.class, Map.of("getUniqueId", id));

        Promise<Integer> promise = Promise.completed(1).thenApply(TaskContext.of(entity), value -> value + 1);
        this.scheduler.retireEntity(id);
        this.scheduler.tick();

        assertTrue(promise.isDone(), "retired entity work must not hang the chain");
        CompletionException failure = assertThrows(CompletionException.class, promise::join);
        assertInstanceOf(EntityRetiredException.class, failure.getCause());
        assertEquals(id, ((EntityRetiredException) failure.getCause()).entityId());
    }

    @Test
    public void regionContinuationRunsOnTheRegionQueue() {
        Location location = new Location(null, 10, 64, -20);

        Promise<String> promise = Promise.completed("x").thenApply(TaskContext.of(location), value -> value + "!");

        assertFalse(promise.isDone());
        this.scheduler.tick();
        assertEquals("x!", promise.join());
        assertEquals(List.of("region:?@10,64,-20"), this.scheduler.executed());
    }

    @Test
    public void supplyingOnAsyncContextQueuesAsync() {
        Promise<Integer> promise = Promise.supplying(TaskContext.async(), () -> 5);

        assertEquals(1, this.scheduler.pendingAsync());
        this.scheduler.runAsync();
        assertEquals(5, promise.join());
    }

    @Test
    public void exceptionallyOnContextRecoversAfterDelay() {
        Promise<String> promise = Promise.<String>exceptionally(new IllegalStateException("boom"))
                .exceptionallyDelayed(TaskContext.global(), error -> "recovered", 3L);

        this.scheduler.tick(2);
        assertFalse(promise.isDone());
        this.scheduler.tick();
        assertEquals("recovered", promise.join());
    }

    @Test
    public void thenComposeDelayedDispatchesByThreadContext() {
        Promise<Integer> promise = Promise.completed(2)
                .thenComposeDelayed(ThreadContext.ASYNC, value -> Promise.completed(value * 2), 2L);

        this.scheduler.tick(2);
        assertFalse(promise.isDone(), "delayed async work lands on the async queue");
        this.scheduler.runAsync();
        assertEquals(4, promise.join());
    }

    @Test
    public void thenComposeAsyncChainsSecondPromise() {
        Promise<String> promise = Promise.completed(3)
                .thenComposeAsync(value -> Promise.supplyingAsync(() -> "x" + value));

        assertFalse(promise.isDone());
        this.scheduler.runAll();

        assertEquals("x3", promise.join());
    }
}
