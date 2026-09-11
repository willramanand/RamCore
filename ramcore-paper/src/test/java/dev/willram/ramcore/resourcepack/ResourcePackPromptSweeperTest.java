package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourcePackPromptSweeperTest {
    private FakeScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    @Test
    public void scheduledSweepTimesOutPendingPrompts() {
        ResourcePackPromptTracker tracker = ResourcePackPromptTracker.create();
        UUID player = UUID.randomUUID();
        ResourcePackPrompt prompt = ResourcePackPrompt.builder(URI.create("https://example.com/pack.zip"), "0".repeat(40))
                .timeoutTicks(1)
                .build();
        tracker.track(player, prompt, 0L); // timeoutAtMillis = 50
        assertEquals(1, tracker.pending().size());

        Task task = ResourcePackPromptSweeper.start(tracker, 1L, () -> 10_000L);
        this.scheduler.tick(1);
        this.scheduler.runAll();

        assertTrue(tracker.pending().isEmpty(), "prompt should have timed out");
        assertEquals(ResourcePackPromptStatus.TIMED_OUT,
                tracker.get(player, prompt.id()).orElseThrow().status());
        assertTrue(task.stop());
    }
}
