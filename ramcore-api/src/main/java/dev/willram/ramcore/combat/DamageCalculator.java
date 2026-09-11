package dev.willram.ramcore.combat;

import dev.willram.ramcore.stat.StatSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

import static java.util.Objects.requireNonNull;

/**
 * Turns a base damage plus attacker/defender {@link StatSnapshot}s into a {@link DamageBreakdown},
 * reading the {@link CombatStats} ids: attacker crit chance/damage and lifesteal, defender flat
 * defense and per-element resistance.
 *
 * <p>Order: crit, then resistance ({@code x(1 - resist)}), then flat defense (subtracted, floored at
 * 0), then lifesteal on the final damage. Resistance is clamped to {@code [0, 0.95]} so damage is
 * never fully negated by resistance alone. Pass an explicit crit roll for deterministic tests.</p>
 */
public final class DamageCalculator {
    private static final double MAX_RESIST = 0.95D;

    private final DoubleSupplier critRoll;

    /** A calculator that rolls crits with {@link ThreadLocalRandom}. */
    public DamageCalculator() {
        this(() -> ThreadLocalRandom.current().nextDouble());
    }

    /**
     * A calculator with an explicit crit roll source returning {@code [0, 1)}. A hit crits when the
     * roll is below the attacker's crit chance.
     *
     * @param critRoll the roll source
     */
    public DamageCalculator(@NotNull DoubleSupplier critRoll) {
        this.critRoll = requireNonNull(critRoll, "critRoll");
    }

    /**
     * Calculates untyped damage.
     *
     * @param base     the base damage
     * @param attacker the attacker's stats
     * @param defender the defender's stats
     * @return the breakdown
     */
    @NotNull
    public DamageBreakdown calculate(double base, @NotNull StatSnapshot attacker, @NotNull StatSnapshot defender) {
        return calculate(base, attacker, defender, null);
    }

    /**
     * Calculates damage of an optional element.
     *
     * @param base     the base damage
     * @param attacker the attacker's stats
     * @param defender the defender's stats
     * @param element  the element for resistance, or {@code null} for untyped
     * @return the breakdown
     */
    @NotNull
    public DamageBreakdown calculate(double base, @NotNull StatSnapshot attacker, @NotNull StatSnapshot defender,
                                     @Nullable String element) {
        requireNonNull(attacker, "attacker");
        requireNonNull(defender, "defender");

        double amount = Math.max(0.0D, base);

        boolean crit = false;
        double critChance = attacker.value(CombatStats.CRIT_CHANCE);
        if (critChance > 0.0D && this.critRoll.getAsDouble() < critChance) {
            crit = true;
            amount *= 1.0D + Math.max(0.0D, attacker.value(CombatStats.CRIT_DAMAGE));
        }
        double afterCrit = amount;

        double resisted = 0.0D;
        if (element != null) {
            double resist = clamp(defender.value(CombatStats.resistance(element)), 0.0D, MAX_RESIST);
            resisted = amount * resist;
            amount -= resisted;
        }

        double defense = Math.max(0.0D, defender.value(CombatStats.DEFENSE));
        double mitigated = Math.min(amount, defense);
        amount -= mitigated;

        double finalDamage = Math.max(0.0D, amount);
        double lifesteal = Math.max(0.0D, attacker.value(CombatStats.LIFESTEAL));
        double healed = finalDamage * lifesteal;

        return new DamageBreakdown(base, element, crit, afterCrit, resisted, mitigated, finalDamage, healed);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
