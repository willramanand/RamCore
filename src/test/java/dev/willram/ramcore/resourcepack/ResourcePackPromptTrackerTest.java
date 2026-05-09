package dev.willram.ramcore.resourcepack;

import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.junit.Test;

import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ResourcePackPromptTrackerTest {

    @Test
    public void statusEventsUpdateTrackedPrompt() {
        ResourcePackPromptTracker tracker = ResourcePackPromptTracker.create();
        UUID playerId = UUID.randomUUID();
        UUID packId = UUID.randomUUID();
        ResourcePackPrompt prompt = prompt(packId, 100L);

        tracker.track(playerId, prompt, 1_000L);
        tracker.handle(playerId, packId, PlayerResourcePackStatusEvent.Status.ACCEPTED, 1_100L);
        tracker.handle(playerId, packId, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED, 1_200L);

        ResourcePackRequest request = tracker.get(playerId, packId).orElseThrow();
        assertEquals(ResourcePackPromptStatus.LOADED, request.status());
        assertTrue(request.terminal());
    }

    @Test
    public void timeoutSweepMarksPendingPrompts() {
        ResourcePackPromptTracker tracker = ResourcePackPromptTracker.create();
        UUID playerId = UUID.randomUUID();
        UUID packId = UUID.randomUUID();

        tracker.track(playerId, prompt(packId, 20L), 1_000L);

        assertEquals(1, tracker.sweepTimeouts(2_001L).size());
        assertEquals(ResourcePackPromptStatus.TIMED_OUT, tracker.get(playerId, packId).orElseThrow().status());
    }

    private static ResourcePackPrompt prompt(UUID packId, long timeoutTicks) {
        return ResourcePackPrompt.builder(URI.create("https://example.com/pack.zip"), "0123456789abcdef0123456789abcdef01234567")
                .id(packId)
                .timeoutTicks(timeoutTicks)
                .build();
    }
}
