package dev.willram.ramcore.combat;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
    }

    @NotNull
    public static Builder amount(double amount) {
        return new Builder().amount(amount);
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

        public Builder amount(double amount) {
            this.amount = amount;
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
