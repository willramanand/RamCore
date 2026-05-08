package dev.willram.ramcore.scheduler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class SchedulerFacadeTest {

    @Test
    public void globalAndSyncContextsAreSameAnchor() {
        assertSame(TaskContext.global(), TaskContext.sync());
        assertEquals(TaskContext.Type.GLOBAL, TaskContext.global().type());
        assertTrue(TaskContext.global().globalContext());
        assertEquals("global", TaskContext.global().description());
    }

    @Test
    public void asyncContextIsInspectable() {
        TaskContext context = TaskContext.async();

        assertEquals(TaskContext.Type.ASYNC, context.type());
        assertTrue(context.asyncContext());
        assertEquals("async", context.description());
        assertEquals("TaskContext[async]", context.toString());
    }

    @Test
    public void forContextReturnsSharedSchedulersForGlobalAndAsync() {
        assertSame(Schedulers.sync(), Schedulers.forContext(TaskContext.global()));
        assertSame(Schedulers.async(), Schedulers.forContext(TaskContext.async()));
        assertSame(Schedulers.sync(), Schedulers.forGlobal());
        assertSame(Schedulers.async(), Schedulers.forAsync());
    }
}
