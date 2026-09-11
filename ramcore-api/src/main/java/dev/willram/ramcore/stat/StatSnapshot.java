package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

import static java.util.Objects.requireNonNull;

/**
 * An immutable, computed view of every registered stat for one player at one instant.
 *
 * <p>Each stat starts from its {@link Stat#base()}. All {@link StatOperation#ADD} modifiers are
 * summed onto the base, then the result is scaled by {@code 1 + sum(MULTIPLY amounts)}, then clamped
 * to the stat's range. Modifiers whose stat id is not registered are ignored, so the snapshot is
 * always bounded by the registry.</p>
 */
public final class StatSnapshot {
    private static final StatSnapshot EMPTY = new StatSnapshot(Map.of());

    private final Map<ContentId, Double> values;

    private StatSnapshot(@NotNull Map<ContentId, Double> values) {
        this.values = Map.copyOf(values);
    }

    /** An empty snapshot with no values. */
    @NotNull
    public static StatSnapshot empty() {
        return EMPTY;
    }

    /**
     * Computes a snapshot by applying the collected modifiers to every stat in the registry.
     *
     * @param registry  the stat definitions
     * @param modifiers the collected modifiers
     * @return the snapshot
     */
    @NotNull
    public static StatSnapshot compute(@NotNull StatRegistry registry, @NotNull Collection<StatModifier> modifiers) {
        requireNonNull(registry, "registry");
        requireNonNull(modifiers, "modifiers");

        Map<ContentId, Double> adds = new HashMap<>();
        Map<ContentId, Double> muls = new HashMap<>();
        for (StatModifier modifier : modifiers) {
            if (!registry.contains(modifier.statId())) {
                continue;
            }
            switch (modifier.operation()) {
                case ADD -> adds.merge(modifier.statId(), modifier.amount(), Double::sum);
                case MULTIPLY -> muls.merge(modifier.statId(), modifier.amount(), Double::sum);
            }
        }

        Map<ContentId, Double> values = new HashMap<>();
        for (ContentId id : registry.ids()) {
            Stat stat = registry.require(id);
            double additive = stat.base() + adds.getOrDefault(id, 0.0D);
            double scaled = additive * (1.0D + muls.getOrDefault(id, 0.0D));
            values.put(id, stat.clamp(scaled));
        }
        return new StatSnapshot(values);
    }

    /**
     * The computed value for a stat, or {@code 0.0} when the stat is not present.
     *
     * @param id the stat id
     * @return the value, or {@code 0.0}
     */
    public double value(@NotNull ContentId id) {
        return this.values.getOrDefault(id, 0.0D);
    }

    /**
     * The computed value for a stat, if present.
     *
     * @param id the stat id
     * @return the value, or empty
     */
    @NotNull
    public OptionalDouble get(@NotNull ContentId id) {
        Double value = this.values.get(id);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /** Every computed value, keyed by stat id. */
    @NotNull
    public Map<ContentId, Double> values() {
        return this.values;
    }
}
