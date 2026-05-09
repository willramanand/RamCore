package dev.willram.ramcore.cooldown;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CooldownTrackerTest {

    @Test
    public void testReturnsStructuredResults() {
        CooldownTracker<String> tracker = CooldownTracker.create(Cooldown.of(1, TimeUnit.MINUTES));

        CooldownResult<String> first = tracker.test("cast");
        CooldownResult<String> second = tracker.test("cast");

        assertTrue(first.allowed());
        assertFalse(second.allowed());
        assertTrue(second.remainingMillis() > 0L);
        assertEquals("cast", second.key());
    }

    @Test
    public void groupedKeysSeparateActions() {
        CooldownTracker<CooldownKey> tracker = Cooldowns.grouped(Cooldown.of(1, TimeUnit.MINUTES));
        CooldownKey first = CooldownKey.of("spell.fire", "player");
        CooldownKey second = CooldownKey.of("spell.ice", "player");

        assertTrue(tracker.test(first).allowed());
        assertTrue(tracker.test(second).allowed());
        assertTrue(tracker.test(first).denied());
    }

    @Test
    public void sweepExpiredRemovesAndCallsListeners() {
        CooldownTracker<String> tracker = CooldownTracker.create(Cooldown.of(1, TimeUnit.MILLISECONDS));
        List<String> expired = new ArrayList<>();
        tracker.onExpire((key, cooldown) -> expired.add(key));

        tracker.reset("dash", 1L);
        List<String> swept = tracker.sweepExpired();

        assertEquals(List.of("dash"), swept);
        assertEquals(List.of("dash"), expired);
        assertFalse(tracker.keys().contains("dash"));
    }

    @Test
    public void actionThrottleUsesActionGroups() {
        ActionThrottle<String> throttle = ActionThrottle.create(Cooldown.of(1, TimeUnit.MINUTES));

        assertTrue(throttle.test("player", "jump").allowed());
        assertTrue(throttle.test("player", "jump").denied());
        assertTrue(throttle.test("player", "dash").allowed());
    }
}
