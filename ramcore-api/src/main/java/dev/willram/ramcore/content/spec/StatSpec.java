package dev.willram.ramcore.content.spec;

import dev.willram.ramcore.content.ContentDeserializeException;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.stat.Stat;
import dev.willram.ramcore.stat.StatFormat;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

/**
 * A pure description of a stat: base value, clamp range, and display format. Off-server;
 * {@link #toStat(ContentId)} turns it into a {@link Stat}. Config type {@code stats}.
 *
 * @param base   the base value
 * @param min    the lower clamp bound
 * @param max    the upper clamp bound
 * @param format the display format
 */
public record StatSpec(double base, double min, double max, @NotNull StatFormat format) {

    @NotNull
    public static StatSpec deserialize(@NotNull ConfigurationNode node) {
        double base = node.node("base").getDouble(0.0D);
        double min = node.node("min").getDouble(-Double.MAX_VALUE);
        double max = node.node("max").getDouble(Double.MAX_VALUE);

        StatFormat format = StatFormat.DECIMAL;
        String formatName = node.node("format").getString();
        if (formatName != null && !formatName.isBlank()) {
            try {
                format = StatFormat.valueOf(formatName.trim().toUpperCase());
            } catch (IllegalArgumentException unknown) {
                throw new ContentDeserializeException("unknown stat format '" + formatName + "'");
            }
        }

        if (!(Double.isFinite(base) && Double.isFinite(min) && Double.isFinite(max))) {
            throw new ContentDeserializeException("stat base/min/max must be finite numbers");
        }
        if (min > max) {
            throw new ContentDeserializeException("stat min " + min + " is greater than max " + max);
        }
        if (base < min || base > max) {
            throw new ContentDeserializeException("stat base " + base + " is outside [" + min + ", " + max + "]");
        }

        return new StatSpec(base, min, max, format);
    }

    /**
     * Converts this spec into a live {@link Stat} with the given id. Range/base validity is enforced
     * by the {@code Stat} constructor.
     *
     * @param id the stat id
     * @return the stat
     */
    @NotNull
    public Stat toStat(@NotNull ContentId id) {
        return new Stat(id, this.base, this.min, this.max, this.format);
    }
}
