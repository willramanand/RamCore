package dev.willram.ramcore.promise;

import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    public void thenComposeAsyncChainsSecondPromise() {
        Promise<String> promise = Promise.completed(3)
                .thenComposeAsync(value -> Promise.supplyingAsync(() -> "x" + value));

        assertFalse(promise.isDone());
        this.scheduler.runAll();

        assertEquals("x3", promise.join());
    }
}
