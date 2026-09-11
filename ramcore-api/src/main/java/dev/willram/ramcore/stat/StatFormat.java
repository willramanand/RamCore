package dev.willram.ramcore.stat;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * How a stat value is rendered for display. Purely presentational; the stored value is always a
 * raw {@code double}.
 */
public enum StatFormat {

    /** Rounded to the nearest whole number, e.g. {@code 42}. */
    INTEGER {
        @Override
        @NotNull
        public String format(double value) {
            return Long.toString(Math.round(value));
        }
    },

    /** One decimal place, e.g. {@code 42.5}. */
    DECIMAL {
        @Override
        @NotNull
        public String format(double value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
    },

    /** The value treated as a fraction and shown as a percentage, e.g. {@code 0.25 -> 25%}. */
    PERCENT {
        @Override
        @NotNull
        public String format(double value) {
            return Long.toString(Math.round(value * 100.0D)) + "%";
        }
    };

    /**
     * Renders a raw stat value.
     *
     * @param value the raw value
     * @return the display string
     */
    @NotNull
    public abstract String format(double value);
}
