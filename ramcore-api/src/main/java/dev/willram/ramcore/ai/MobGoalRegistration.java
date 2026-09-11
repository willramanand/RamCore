package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Tracked goal registration with priority and pause state.
 */
public record MobGoalRegistration<T extends Mob>(
        int priority,
        @NotNull Goal<T> goal,
        boolean paused
) {
    @NotNull
    public GoalKey<T> key() {
        return this.goal.getKey();
    }

    @NotNull
    public MobGoalRegistration<T> paused(boolean paused) {
        return new MobGoalRegistration<>(this.priority, this.goal, paused);
    }
}
