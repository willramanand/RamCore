package dev.willram.ramcore.combat;

import dev.willram.ramcore.stat.StatSnapshot;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Controlled damage application with Paper-exposed invulnerability frame hooks.
 */
public final class DamageProfile {
    private final double amount;
    private final Entity damager;
    private final DamageSource source;
    private final boolean clearNoDamageTicks;
    private final Integer noDamageTicksAfter;
    private final Integer maximumNoDamageTicksAfter;
    private final Float hurtDirection;
    private final DamageBreakdown breakdown;

    private DamageProfile(Builder builder) {
        if (builder.amount < 0.0d) {
            throw new IllegalArgumentException("damage amount must not be negative");
        }
        this.amount = builder.amount;
        this.damager = builder.damager;
        this.source = builder.source;
        this.clearNoDamageTicks = builder.clearNoDamageTicks;
        this.noDamageTicksAfter = builder.noDamageTicksAfter;
        this.maximumNoDamageTicksAfter = builder.maximumNoDamageTicksAfter;
        this.hurtDirection = builder.hurtDirection;
        this.breakdown = builder.breakdown;
    }

    @NotNull
    public static Builder amount(double amount) {
        return new Builder().amount(amount);
    }

    /** The final damage this profile applies. */
    public double amount() {
        return this.amount;
    }

    /**
     * The stat breakdown behind this profile's amount when built via
     * {@link Builder#withStats(StatSnapshot, StatSnapshot)}; empty otherwise. Callers use it to apply
     * lifesteal, since {@link #apply(Damageable)} damages only the target.
     *
     * @return the breakdown, or empty
     */
    @NotNull
    public Optional<DamageBreakdown> breakdown() {
        return Optional.ofNullable(this.breakdown);
    }

    public void apply(@NotNull Damageable target) {
        Objects.requireNonNull(target, "target");
        if (target instanceof LivingEntity living && this.clearNoDamageTicks) {
            living.setNoDamageTicks(0);
        }
        if (this.source != null) {
            target.damage(this.amount, this.source);
        } else if (this.damager != null) {
            target.damage(this.amount, this.damager);
        } else {
            target.damage(this.amount);
        }
        if (target instanceof LivingEntity living) {
            if (this.noDamageTicksAfter != null) {
                living.setNoDamageTicks(this.noDamageTicksAfter);
            }
            if (this.maximumNoDamageTicksAfter != null) {
                living.setMaximumNoDamageTicks(this.maximumNoDamageTicksAfter);
            }
            if (this.hurtDirection != null) {
                living.setHurtDirection(this.hurtDirection);
            }
        }
    }

    public static final class Builder {
        private double amount;
        private Entity damager;
        private DamageSource source;
        private boolean clearNoDamageTicks;
        private Integer noDamageTicksAfter;
        private Integer maximumNoDamageTicksAfter;
        private Float hurtDirection;
        private DamageBreakdown breakdown;
        private DamageCalculator calculator;

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        /**
         * The {@link DamageCalculator} used by {@link #withStats(StatSnapshot, StatSnapshot)}. When
         * unset a default (randomised crit) calculator is used. Set an explicit one for deterministic
         * crits.
         *
         * @param calculator the calculator
         * @return this builder
         */
        @NotNull
        public Builder calculator(@NotNull DamageCalculator calculator) {
            this.calculator = Objects.requireNonNull(calculator, "calculator");
            return this;
        }

        /**
         * Runs the {@link DamageCalculator} over the current amount and the two snapshots, sets the
         * amount to the mitigated final damage, and records the {@link DamageBreakdown} (see
         * {@link DamageProfile#breakdown()}). Callers that never call this are unaffected.
         *
         * @param attacker the attacker's stats
         * @param defender the defender's stats
         * @return this builder
         */
        @NotNull
        public Builder withStats(@NotNull StatSnapshot attacker, @NotNull StatSnapshot defender) {
            return withStats(attacker, defender, null);
        }

        /**
         * As {@link #withStats(StatSnapshot, StatSnapshot)}, with an element for resistance.
         *
         * @param attacker the attacker's stats
         * @param defender the defender's stats
         * @param element  the damage element, or {@code null} for untyped
         * @return this builder
         */
        @NotNull
        public Builder withStats(@NotNull StatSnapshot attacker, @NotNull StatSnapshot defender,
                                 @Nullable String element) {
            DamageCalculator calc = this.calculator != null ? this.calculator : new DamageCalculator();
            this.breakdown = calc.calculate(this.amount, attacker, defender, element);
            this.amount = this.breakdown.finalDamage();
            return this;
        }

        public Builder damager(@Nullable Entity damager) {
            this.damager = damager;
            this.source = null;
            return this;
        }

        public Builder source(@Nullable DamageSource source) {
            this.source = source;
            this.damager = null;
            return this;
        }

        public Builder clearNoDamageTicks(boolean clearNoDamageTicks) {
            this.clearNoDamageTicks = clearNoDamageTicks;
            return this;
        }

        public Builder noDamageTicksAfter(int ticks) {
            this.noDamageTicksAfter = ticks;
            return this;
        }

        public Builder maximumNoDamageTicksAfter(int ticks) {
            this.maximumNoDamageTicksAfter = ticks;
            return this;
        }

        public Builder hurtDirection(float hurtDirection) {
            this.hurtDirection = hurtDirection;
            return this;
        }

        public DamageProfile build() {
            return new DamageProfile(this);
        }
    }
}
