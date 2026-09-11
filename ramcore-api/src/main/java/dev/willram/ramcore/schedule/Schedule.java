package dev.willram.ramcore.schedule;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Computes when a job should next run. Built-ins: {@link #cron(String)}, {@link #every(Duration)},
 * and {@link #at(LocalTime)} (daily).
 */
@FunctionalInterface
public interface Schedule {

    /**
     * The next run strictly after {@code after}, in the given zone.
     *
     * @param after the lower bound (exclusive)
     * @param zone  the time zone
     * @return the next run, or empty if there is none
     */
    @NotNull
    Optional<Instant> nextAfter(@NotNull Instant after, @NotNull ZoneId zone);

    /** A cron schedule (5 fields). */
    @NotNull
    static Schedule cron(@NotNull String expression) {
        CronExpression cron = CronExpression.parse(expression);
        return (after, zone) -> cron.next(ZonedDateTime.ofInstant(after, zone)).map(ZonedDateTime::toInstant);
    }

    /** A fixed-interval schedule: every {@code interval} from the last run. */
    @NotNull
    static Schedule every(@NotNull Duration interval) {
        requireNonNull(interval, "interval");
        RamPreconditions.checkArgument(!interval.isZero() && !interval.isNegative(),
                "interval must be positive", "pass a positive interval");
        return (after, zone) -> Optional.of(after.plus(interval));
    }

    /** A daily schedule at a wall-clock time in the job's zone. */
    @NotNull
    static Schedule at(@NotNull LocalTime time) {
        requireNonNull(time, "time");
        return (after, zone) -> {
            ZonedDateTime from = ZonedDateTime.ofInstant(after, zone);
            ZonedDateTime candidate = from.toLocalDate().atTime(time).atZone(zone);
            if (!candidate.toInstant().isAfter(after)) {
                candidate = candidate.plusDays(1);
            }
            return Optional.of(candidate.toInstant());
        };
    }
}
