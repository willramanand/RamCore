package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A single contribution to a stat, produced by a {@link StatSource}.
 *
 * @param statId    the stat this modifier affects
 * @param operation how the amount combines (see {@link StatOperation})
 * @param amount    the modifier amount; for {@link StatOperation#MULTIPLY} a fraction (0.10 = +10%)
 * @param sourceKey a stable key identifying where this modifier came from (item slot, buff id, ...);
 *                  used for diagnostics and de-duplication, never blank
 */
public record StatModifier(@NotNull ContentId statId, @NotNull StatOperation operation, double amount,
                           @NotNull String sourceKey) {

    public StatModifier {
        requireNonNull(statId, "statId");
        requireNonNull(operation, "operation");
        RamPreconditions.notBlank(sourceKey, "sourceKey");
        RamPreconditions.checkArgument(Double.isFinite(amount),
                "stat modifier amount must be finite (was " + amount + ")",
                "pass a finite amount to StatModifier");
    }

    /**
     * An additive modifier.
     *
     * @param statId    the stat id
     * @param amount    the amount to add
     * @param sourceKey the source key
     * @return the modifier
     */
    @NotNull
    public static StatModifier add(@NotNull ContentId statId, double amount, @NotNull String sourceKey) {
        return new StatModifier(statId, StatOperation.ADD, amount, sourceKey);
    }

    /**
     * A multiplicative modifier. An amount of {@code 0.10} scales the additive sum by +10%.
     *
     * @param statId    the stat id
     * @param amount    the fraction to scale by
     * @param sourceKey the source key
     * @return the modifier
     */
    @NotNull
    public static StatModifier multiply(@NotNull ContentId statId, double amount, @NotNull String sourceKey) {
        return new StatModifier(statId, StatOperation.MULTIPLY, amount, sourceKey);
    }
}
