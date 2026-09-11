package dev.willram.ramcore.session;

import dev.willram.ramcore.testkit.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SessionRecorderTest {
    private final UUID player = UUID.randomUUID();

    @Test
    public void noopRecordsNothing() {
        SessionRecorder recorder = SessionRecorder.NOOP;
        recorder.record(player, SessionEventType.REWARD, "x");
        assertTrue(recorder.timeline(player, 10).isEmpty());
        assertFalse(recorder.enabled());
    }

    @Test
    public void ringKeepsOnlyTheMostRecentUpToCapacity() {
        FakeClock clock = FakeClock.epoch();
        AtomicLong tick = new AtomicLong();
        SessionRecorder recorder = SessionRecorder.ring(3, clock, tick::getAndIncrement);

        for (int i = 0; i < 5; i++) {
            recorder.record(player, SessionEventType.CUSTOM, "e" + i);
            clock.advance(Duration.ofSeconds(1));
        }
        List<SessionEvent> timeline = recorder.timeline(player, 10);
        assertEquals(3, timeline.size(), "capped at capacity");
        assertEquals(List.of("e2", "e3", "e4"), timeline.stream().map(SessionEvent::detail).toList());
        assertEquals(2, timeline.get(0).tick(), "oldest kept event tick");
    }

    @Test
    public void timelineLimitReturnsTheLastN() {
        SessionRecorder recorder = SessionRecorder.ring(10, FakeClock.epoch(), () -> 0L);
        for (int i = 0; i < 6; i++) {
            recorder.record(player, SessionEventType.CUSTOM, "e" + i);
        }
        assertEquals(List.of("e4", "e5"), recorder.timeline(player, 2).stream().map(SessionEvent::detail).toList());
        assertTrue(recorder.timeline(player, 0).isEmpty());
        assertTrue(recorder.timeline(UUID.randomUUID(), 5).isEmpty());
    }

    @Test
    public void clearDropsThePlayerBuffer() {
        SessionRecorder recorder = SessionRecorder.ring(4);
        recorder.record(player, SessionEventType.LOOT, "sword");
        recorder.clear(player);
        assertTrue(recorder.timeline(player, 10).isEmpty());
    }

    @Test
    public void formatsLines() {
        SessionRecorder recorder = SessionRecorder.ring(4, FakeClock.epoch(), () -> 42L);
        recorder.record(player, SessionEventType.REWARD, "money=100");
        List<String> lines = SessionTimelines.lines(recorder.timeline(player, 10));
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("tick=42"));
        assertTrue(lines.get(0).contains("REWARD"));
        assertTrue(lines.get(0).contains("money=100"));
        assertTrue(lines.get(0).startsWith("00:00:00"));
    }
}
