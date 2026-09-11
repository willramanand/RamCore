package dev.willram.ramcore.combat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The step-by-step result of a {@link DamageCalculator} run: the base damage, whether it crit, the
 * amount removed by elemental resistance and by flat defense, the final damage dealt, and the health
 * the attacker recovers from lifesteal.
 *
 * @param base            the input damage before any stat
 * @param element         the damage element, or {@code null} if untyped
 * @param crit            whether the hit crit
 * @param afterCrit       damage after the crit multiplier (equal to {@code base} when no crit)
 * @param resistedAmount  damage removed by elemental resistance
 * @param mitigatedAmount damage removed by flat defense
 * @param finalDamage     the damage actually dealt (never negative)
 * @param lifestealHealed health returned to the attacker
 */
public record DamageBreakdown(double base, @Nullable String element, boolean crit, double afterCrit,
                              double resistedAmount, double mitigatedAmount, double finalDamage,
                              double lifestealHealed) {

    /**
     * A breakdown for untyped damage with no stats applied: the base passes through unchanged.
     *
     * @param base the base damage
     * @return the breakdown
     */
    @NotNull
    public static DamageBreakdown passthrough(double base) {
        double finalDamage = Math.max(0.0D, base);
        return new DamageBreakdown(base, null, false, base, 0.0D, 0.0D, finalDamage, 0.0D);
    }
}
