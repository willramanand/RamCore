package dev.willram.ramcore.testkit;

import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Manually advanced {@link Clock} for tests that depend on wall-clock time.
 */
public final class FakeClock extends Clock {
    private Instant instant;
    private final ZoneId zone;

    public FakeClock(@NotNull Instant instant) {
        this(instant, ZoneOffset.UTC);
    }

    private FakeClock(Instant instant, ZoneId zone) {
        this.instant = Objects.requireNonNull(instant, "instant");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /** A clock starting at the Unix epoch. */
    @NotNull
    public static FakeClock epoch() {
        return new FakeClock(Instant.EPOCH);
    }

    @NotNull
    public static FakeClock at(@NotNull Instant instant) {
        return new FakeClock(instant);
    }

    public void advance(@NotNull Duration duration) {
        this.instant = this.instant.plus(Objects.requireNonNull(duration, "duration"));
    }

    public void set(@NotNull Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant");
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new FakeClock(this.instant, zone);
    }

    @Override
    public Instant instant() {
        return this.instant;
    }
}
