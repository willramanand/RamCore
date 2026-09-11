package dev.willram.ramcore.schedule;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A standard 5-field cron expression ({@code minute hour day-of-month month day-of-week}), parsed
 * in-house (no dependency). Each field supports {@code *}, single values, ranges {@code a-b}, lists
 * {@code a,b,c}, and steps ({@code a-b/n} or {@code &#42;/n}). Day-of-week is {@code 0-6} (0 or 7 = Sunday).
 *
 * <p>When both day-of-month and day-of-week are restricted, a day matches if <em>either</em> matches
 * (the traditional cron rule).</p>
 */
public final class CronExpression {
    private final BitSet minutes;
    private final BitSet hours;
    private final BitSet daysOfMonth;
    private final BitSet months;
    private final BitSet daysOfWeek;
    private final boolean domRestricted;
    private final boolean dowRestricted;
    private final String expression;

    private CronExpression(String expression, BitSet minutes, BitSet hours, BitSet daysOfMonth, BitSet months,
                           BitSet daysOfWeek, boolean domRestricted, boolean dowRestricted) {
        this.expression = expression;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
        this.domRestricted = domRestricted;
        this.dowRestricted = dowRestricted;
    }

    /**
     * Parses a 5-field cron expression.
     *
     * @param expression the expression, e.g. {@code "0 4 * * 1"}
     * @return the parsed expression
     * @throws IllegalArgumentException if malformed
     */
    @NotNull
    public static CronExpression parse(@NotNull String expression) {
        requireNonNull(expression, "expression");
        String[] fields = expression.trim().split("\\s+");
        if (fields.length != 5) {
            throw new IllegalArgumentException("cron expression must have 5 fields (was " + fields.length + "): " + expression);
        }
        BitSet minutes = field(fields[0], 0, 59, "minute");
        BitSet hours = field(fields[1], 0, 23, "hour");
        BitSet dom = field(fields[2], 1, 31, "day-of-month");
        BitSet months = field(fields[3], 1, 12, "month");
        BitSet dow = dayOfWeek(fields[4]);
        return new CronExpression(expression, minutes, hours, dom, months, dow,
                !fields[2].equals("*"), !fields[4].equals("*"));
    }

    private static BitSet dayOfWeek(String spec) {
        BitSet set = field(spec, 0, 7, "day-of-week");
        if (set.get(7)) {
            set.set(0); // 7 == Sunday == 0
            set.clear(7);
        }
        return set;
    }

    private static BitSet field(String spec, int min, int max, String name) {
        BitSet set = new BitSet(max + 1);
        for (String part : spec.split(",")) {
            int step = 1;
            String range = part;
            int slash = part.indexOf('/');
            if (slash >= 0) {
                range = part.substring(0, slash);
                step = parse(part.substring(slash + 1), name);
                if (step <= 0) {
                    throw new IllegalArgumentException("cron " + name + " step must be positive: " + part);
                }
            }
            int lo;
            int hi;
            if (range.equals("*")) {
                lo = min;
                hi = max;
            } else if (range.contains("-")) {
                int dash = range.indexOf('-');
                lo = parse(range.substring(0, dash), name);
                hi = parse(range.substring(dash + 1), name);
            } else {
                lo = hi = parse(range, name);
                if (slash >= 0) {
                    hi = max; // "a/n" means from a to max stepping n
                }
            }
            if (lo < min || hi > max || lo > hi) {
                throw new IllegalArgumentException("cron " + name + " out of range [" + min + "," + max + "]: " + part);
            }
            for (int value = lo; value <= hi; value += step) {
                set.set(value);
            }
        }
        if (set.isEmpty()) {
            throw new IllegalArgumentException("cron " + name + " matches nothing: " + spec);
        }
        return set;
    }

    private static int parse(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("cron " + name + " is not a number: " + value);
        }
    }

    /**
     * Whether the given time matches (to the minute).
     *
     * @param time the time
     * @return true if it matches
     */
    public boolean matches(@NotNull ZonedDateTime time) {
        if (!this.minutes.get(time.getMinute()) || !this.hours.get(time.getHour())
                || !this.months.get(time.getMonthValue())) {
            return false;
        }
        boolean domMatch = this.daysOfMonth.get(time.getDayOfMonth());
        boolean dowMatch = this.daysOfWeek.get(time.getDayOfWeek().getValue() % 7); // Mon=1..Sun=7 -> Sun=0
        if (this.domRestricted && this.dowRestricted) {
            return domMatch || dowMatch;
        }
        return domMatch && dowMatch;
    }

    /**
     * The next time strictly after {@code after} that matches, searching up to ~4 years.
     *
     * @param after the lower bound (exclusive)
     * @return the next match, or empty if none within the search window
     */
    @NotNull
    public Optional<ZonedDateTime> next(@NotNull ZonedDateTime after) {
        ZonedDateTime candidate = after.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        int limit = 4 * 366 * 24 * 60; // ~4 years of minutes
        for (int i = 0; i < limit; i++) {
            if (matches(candidate)) {
                return Optional.of(candidate);
            }
            candidate = candidate.plusMinutes(1);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return this.expression;
    }
}
