package dev.willram.ramcore.kotlin

import dev.willram.ramcore.promise.Promise
import dev.willram.ramcore.testkit.FakeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RamCoroutinesTest {
    private lateinit var scheduler: FakeScheduler

    @BeforeTest
    fun setUp() {
        scheduler = FakeScheduler.install()
    }

    @AfterTest
    fun tearDown() {
        scheduler.close()
    }

    @Test
    fun awaitReturnsTheValue() = runBlocking {
        assertEquals("hello", Promise.completed("hello").await())
    }

    @Test
    fun awaitPropagatesTheFailureCause() = runBlocking {
        val promise = Promise.exceptionally<String>(IllegalStateException("boom"))
        val error = assertFailsWith<IllegalStateException> { promise.await() }
        assertEquals("boom", error.message)
    }

    @Test
    fun cancellingTheCoroutineCancelsThePromise() = runBlocking {
        val promise: Promise<String> = Promise.empty()
        val started = CompletableDeferred<Unit>()
        val job = launch {
            started.complete(Unit)
            promise.await()
        }
        started.await()
        job.cancelAndJoin()
        assertTrue(promise.isCancelled(), "promise cancelled when the awaiting coroutine is cancelled")
    }

    @Test
    fun asyncDispatcherRoutesToTheAsyncQueue() {
        var ran = false
        CoroutineScope(RamDispatchers.async).launch { ran = true }
        assertEquals(1, scheduler.pendingAsync(), "coroutine start queued on the async scheduler")
        assertTrue(!ran)
        scheduler.runAsync()
        assertTrue(ran, "coroutine ran once the async queue was drained")
    }

    @Test
    fun globalDispatcherRunsInlineOnTheSyncThread() {
        // the test thread is the FakeScheduler sync thread, so global work is not re-dispatched
        var ran = false
        CoroutineScope(RamDispatchers.global).launch { ran = true }
        assertTrue(ran, "already on the sync thread: ran inline, no dispatch")
        assertEquals(0, scheduler.pendingSync())
    }
}
