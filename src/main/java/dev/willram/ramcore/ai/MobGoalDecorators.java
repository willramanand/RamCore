package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Common decorators for Paper goals.
 */
public final class MobGoalDecorators {

    @NotNull
    public static <T extends Mob> Goal<T> condition(@NotNull Goal<T> goal, @NotNull BooleanSupplier condition) {
        return wrap(goal, condition, () -> true);
    }

    @NotNull
    public static <T extends Mob> Goal<T> randomChance(@NotNull Goal<T> goal, double chance, @NotNull Random random) {
        return condition(goal, () -> random.nextDouble() < chance);
    }

    @NotNull
    public static <T extends Mob> Goal<T> cooldown(@NotNull Goal<T> goal, @NotNull Duration cooldown, @NotNull Clock clock) {
        return new DelegatingGoal<>(goal) {
            private Instant nextActivation = Instant.MIN;

            @Override
            public boolean shouldActivate() {
                return !clock.instant().isBefore(this.nextActivation) && goal.shouldActivate();
            }

            @Override
            public void stop() {
                goal.stop();
                this.nextActivation = clock.instant().plus(cooldown);
            }
        };
    }

    @NotNull
    public static <T extends Mob> Goal<T> timeout(@NotNull Goal<T> goal, @NotNull Duration timeout, @NotNull Clock clock) {
        return new DelegatingGoal<>(goal) {
            private Instant startedAt = Instant.MIN;

            @Override
            public void start() {
                this.startedAt = clock.instant();
                goal.start();
            }

            @Override
            public boolean shouldStayActive() {
                return clock.instant().isBefore(this.startedAt.plus(timeout)) && goal.shouldStayActive();
            }
        };
    }

    @NotNull
    public static <T extends Mob> Goal<T> distanceGate(@NotNull Goal<T> goal, @NotNull Entity source,
                                                       @NotNull Entity target, double maxDistance) {
        double maxDistanceSquared = maxDistance * maxDistance;
        return condition(goal, () -> source.getWorld().equals(target.getWorld())
                && source.getLocation().distanceSquared(target.getLocation()) <= maxDistanceSquared);
    }

    @NotNull
    public static <T extends Mob> Goal<T> lineOfSightGate(@NotNull Goal<T> goal, @NotNull LivingEntity source,
                                                          @NotNull Entity target) {
        return condition(goal, () -> source.hasLineOfSight(target));
    }

    @NotNull
    public static <T extends Mob> Goal<T> healthGate(@NotNull Goal<T> goal, @NotNull Damageable entity,
                                                     double minimumHealth) {
        return condition(goal, () -> entity.getHealth() >= minimumHealth);
    }

    private static <T extends Mob> Goal<T> wrap(Goal<T> goal, BooleanSupplier activateGate, BooleanSupplier stayGate) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(activateGate, "activateGate");
        Objects.requireNonNull(stayGate, "stayGate");
        return new DelegatingGoal<>(goal) {
            @Override
            public boolean shouldActivate() {
                return activateGate.getAsBoolean() && goal.shouldActivate();
            }

            @Override
            public boolean shouldStayActive() {
                return stayGate.getAsBoolean() && goal.shouldStayActive();
            }
        };
    }

    private static class DelegatingGoal<T extends Mob> implements Goal<T> {
        private final Goal<T> delegate;

        private DelegatingGoal(Goal<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public boolean shouldActivate() {
            return this.delegate.shouldActivate();
        }

        @Override
        public boolean shouldStayActive() {
            return this.delegate.shouldStayActive();
        }

        @Override
        public void start() {
            this.delegate.start();
        }

        @Override
        public void stop() {
            this.delegate.stop();
        }

        @Override
        public void tick() {
            this.delegate.tick();
        }

        @Override
        public GoalKey<T> getKey() {
            return this.delegate.getKey();
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return this.delegate.getTypes();
        }
    }

    private MobGoalDecorators() {
    }
}
