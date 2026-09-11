package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A registered stat definition: an identity, a base value everyone starts from, the clamp range a
 * computed value is held to, and how it renders.
 *
 * @param id     the stat id
 * @param base   the value before any modifier is applied
 * @param min    the lowest a computed value may be
 * @param max    the highest a computed value may be
 * @param format how the value is displayed
 */
public record Stat(@NotNull ContentId id, double base, double min, double max, @NotNull StatFormat format) {

    public Stat {
        requireNonNull(id, "id");
        requireNonNull(format, "format");
        RamPreconditions.checkArgument(Double.isFinite(base) && Double.isFinite(min) && Double.isFinite(max),
                "stat '" + id + "' base/min/max must be finite",
                "pass finite base, min, and max to Stat");
        RamPreconditions.checkArgument(min <= max,
                "stat '" + id + "' has min " + min + " greater than max " + max,
                "set min <= max");
        RamPreconditions.checkArgument(base >= min && base <= max,
                "stat '" + id + "' base " + base + " is outside [" + min + ", " + max + "]",
                "set base within the [min, max] range");
    }

    /**
     * A stat with the default full {@code double} clamp range and {@link StatFormat#DECIMAL}.
     *
     * @param id   the stat id
     * @param base the base value
     * @return the stat
     */
    @NotNull
    public static Stat of(@NotNull ContentId id, double base) {
        return new Stat(id, base, -Double.MAX_VALUE, Double.MAX_VALUE, StatFormat.DECIMAL);
    }

    /**
     * Clamps a computed value into this stat's {@code [min, max]} range.
     *
     * @param value the raw value
     * @return the clamped value
     */
    public double clamp(double value) {
        if (value < this.min) {
            return this.min;
        }
        if (value > this.max) {
            return this.max;
        }
        return value;
    }
}
