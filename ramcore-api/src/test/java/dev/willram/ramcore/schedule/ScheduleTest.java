package dev.willram.ramcore.schedule;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ScheduleTest {
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static Instant utc(String iso) {
        return ZonedDateTime.parse(iso).toInstant();
    }

    @Test
    public void cronWeeklyMondayFourAm() {
        // 2026-01-01 is a Thursday; next Monday is Jan 5
        Instant next = Schedule.cron("0 4 * * 1").nextAfter(utc("2026-01-01T00:00:00Z"), UTC).orElseThrow();
        assertEquals(utc("2026-01-05T04:00:00Z"), next);
    }

    @Test
    public void cronStepMinutes() {
        Instant next = Schedule.cron("*/15 * * * *").nextAfter(utc("2026-01-01T12:00:00Z"), UTC).orElseThrow();
        assertEquals(utc("2026-01-01T12:15:00Z"), next);
    }

    @Test
    public void cronDomOrDowWhenBothRestricted() {
        // midnight on the 1st OR any Monday -> next after Thu Jan 1 12:00 is Mon Jan 5 00:00
        Instant next = Schedule.cron("0 0 1 * 1").nextAfter(utc("2026-01-01T12:00:00Z"), UTC).orElseThrow();
        assertEquals(utc("2026-01-05T00:00:00Z"), next);
    }

    @Test
    public void sundayZeroAndSevenEquivalent() {
        Instant from = utc("2026-01-01T00:00:00Z");
        assertEquals(Schedule.cron("0 0 * * 0").nextAfter(from, UTC),
                Schedule.cron("0 0 * * 7").nextAfter(from, UTC));
    }

    @Test
    public void everyInterval() {
        assertEquals(utc("2026-01-01T01:00:00Z"),
                Schedule.every(Duration.ofHours(1)).nextAfter(utc("2026-01-01T00:00:00Z"), UTC).orElseThrow());
    }

    @Test
    public void dailyAt() {
        assertEquals(utc("2026-01-01T09:30:00Z"),
                Schedule.at(LocalTime.of(9, 30)).nextAfter(utc("2026-01-01T08:00:00Z"), UTC).orElseThrow());
        // already passed today -> tomorrow
        assertEquals(utc("2026-01-02T09:30:00Z"),
                Schedule.at(LocalTime.of(9, 30)).nextAfter(utc("2026-01-01T10:00:00Z"), UTC).orElseThrow());
    }

    @Test
    public void cronParseErrors() {
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * *"));      // 4 fields
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("99 * * * *"));   // out of range
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("a * * * *"));    // not a number
    }

    @Test
    public void cronListsAndRanges() {
        CronExpression cron = CronExpression.parse("0 9-17 * * 1-5"); // top of hour, 9am-5pm, weekdays
        assertTrue(cron.matches(ZonedDateTime.parse("2026-01-01T09:00:00Z"))); // Thu 9am
        assertTrue(cron.matches(ZonedDateTime.parse("2026-01-01T17:00:00Z"))); // Thu 5pm
        assertTrue(!cron.matches(ZonedDateTime.parse("2026-01-03T09:00:00Z"))); // Saturday
        assertTrue(!cron.matches(ZonedDateTime.parse("2026-01-01T08:00:00Z"))); // 8am
    }
}
