package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Backend boundary for mob goal operations.
 */
public interface MobGoalBackend {
    <T extends Mob> void addGoal(@NotNull T mob, int priority, @NotNull Goal<T> goal);

    <T extends Mob> void removeGoal(@NotNull T mob, @NotNull Goal<T> goal);

    <T extends Mob> void removeGoal(@NotNull T mob, @NotNull GoalKey<T> key);

    <T extends Mob> void removeAllGoals(@NotNull T mob);

    <T extends Mob> void removeAllGoals(@NotNull T mob, @NotNull GoalType type);

    <T extends Mob> boolean hasGoal(@NotNull T mob, @NotNull GoalKey<T> key);

    @NotNull
    <T extends Mob> Collection<Goal<T>> allGoals(@NotNull T mob);

    @NotNull
    <T extends Mob> Collection<Goal<T>> runningGoals(@NotNull T mob);
}
