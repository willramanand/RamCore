package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Callback-backed Paper goal with debug state.
 */
public final class RamMobGoal<T extends Mob> implements Goal<T> {
    private final GoalKey<T> key;
    private final EnumSet<GoalType> types;
    private final BooleanSupplier activate;
    private final BooleanSupplier stayActive;
    private final Runnable start;
    private final Runnable stop;
    private final Runnable tick;
    private final Clock clock;
    private final MobGoalDebugState debugState = new MobGoalDebugState();

    RamMobGoal(@NotNull GoalKey<T> key, @NotNull EnumSet<GoalType> types, @NotNull BooleanSupplier activate,
               @NotNull BooleanSupplier stayActive, @NotNull Runnable start, @NotNull Runnable stop,
               @NotNull Runnable tick, @NotNull Clock clock) {
        this.key = Objects.requireNonNull(key, "key");
        this.types = EnumSet.copyOf(types);
        this.activate = Objects.requireNonNull(activate, "activate");
        this.stayActive = Objects.requireNonNull(stayActive, "stayActive");
        this.start = Objects.requireNonNull(start, "start");
        this.stop = Objects.requireNonNull(stop, "stop");
        this.tick = Objects.requireNonNull(tick, "tick");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @NotNull
    public MobGoalDebugState debugState() {
        return this.debugState;
    }

    @Override
    public boolean shouldActivate() {
        return this.activate.getAsBoolean();
    }

    @Override
    public boolean shouldStayActive() {
        return this.stayActive.getAsBoolean();
    }

    @Override
    public void start() {
        this.debugState.started(this.clock.instant());
        this.start.run();
    }

    @Override
    public void stop() {
        this.debugState.stopped(this.clock.instant());
        this.stop.run();
    }

    @Override
    public void tick() {
        this.debugState.ticked();
        this.tick.run();
    }

    @Override
    public GoalKey<T> getKey() {
        return this.key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.copyOf(this.types);
    }
}
