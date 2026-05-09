package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Paper {@link MobGoals}-backed goal operations.
 */
public final class PaperMobGoalBackend implements MobGoalBackend {
    private final MobGoals goals;

    public PaperMobGoalBackend(@NotNull MobGoals goals) {
        this.goals = Objects.requireNonNull(goals, "goals");
    }

    @NotNull
    public static PaperMobGoalBackend runtime() {
        return new PaperMobGoalBackend(Bukkit.getMobGoals());
    }

    @Override
    public <T extends Mob> void addGoal(@NotNull T mob, int priority, @NotNull Goal<T> goal) {
        this.goals.addGoal(mob, priority, goal);
    }

    @Override
    public <T extends Mob> void removeGoal(@NotNull T mob, @NotNull Goal<T> goal) {
        this.goals.removeGoal(mob, goal);
    }

    @Override
    public <T extends Mob> void removeGoal(@NotNull T mob, @NotNull GoalKey<T> key) {
        this.goals.removeGoal(mob, key);
    }

    @Override
    public <T extends Mob> void removeAllGoals(@NotNull T mob) {
        this.goals.removeAllGoals(mob);
    }

    @Override
    public <T extends Mob> void removeAllGoals(@NotNull T mob, @NotNull GoalType type) {
        this.goals.removeAllGoals(mob, type);
    }

    @Override
    public <T extends Mob> boolean hasGoal(@NotNull T mob, @NotNull GoalKey<T> key) {
        return this.goals.hasGoal(mob, key);
    }

    @Override
    @NotNull
    public <T extends Mob> Collection<Goal<T>> allGoals(@NotNull T mob) {
        return this.goals.getAllGoals(mob);
    }

    @Override
    @NotNull
    public <T extends Mob> Collection<Goal<T>> runningGoals(@NotNull T mob) {
        return this.goals.getRunningGoals(mob);
    }
}
