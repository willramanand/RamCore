package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Fluent builder for callback-backed mob goals.
 */
public final class RamMobGoalBuilder<T extends Mob> {
    private final GoalKey<T> key;
    private EnumSet<GoalType> types = EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    private BooleanSupplier activate = () -> true;
    private BooleanSupplier stayActive = () -> true;
    private Runnable start = () -> {};
    private Runnable stop = () -> {};
    private Runnable tick = () -> {};
    private Clock clock = Clock.systemUTC();

    RamMobGoalBuilder(@NotNull GoalKey<T> key) {
        this.key = Objects.requireNonNull(key, "key");
    }

    @NotNull
    public RamMobGoalBuilder<T> types(@NotNull GoalType first, GoalType... rest) {
        this.types = rest.length == 0 ? EnumSet.of(first) : EnumSet.of(first, rest);
        return this;
    }

    @NotNull
    public RamMobGoalBuilder<T> activateWhen(@NotNull BooleanSupplier activate) {
        this.activate = Objects.requireNonNull(activate, "activate");
        return this;
    }

    @NotNull
    public RamMobGoalBuilder<T> stayActiveWhen(@NotNull BooleanSupplier stayActive) {
        this.stayActive = Objects.requireNonNull(stayActive, "stayActive");
        return this;
    }

    @NotNull
    public RamMobGoalBuilder<T> onStart(@NotNull Runnable start) {
        this.start = Objects.requireNonNull(start, "start");
        return this;
    }

    @NotNull
    public RamMobGoalBuilder<T> onStop(@NotNull Runnable stop) {
        this.stop = Objects.requireNonNull(stop, "stop");
        return this;
    }

    @NotNull
    public RamMobGoalBuilder<T> onTick(@NotNull Runnable tick) {
        this.tick = Objects.requireNonNull(tick, "tick");
        return this;
    }

    @NotNull
    public RamMobGoalBuilder<T> clock(@NotNull Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        return this;
    }

    @NotNull
    public RamMobGoal<T> build() {
        return new RamMobGoal<>(this.key, this.types, this.activate, this.stayActive, this.start, this.stop, this.tick, this.clock);
    }
}
