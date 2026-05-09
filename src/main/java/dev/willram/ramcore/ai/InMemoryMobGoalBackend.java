package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic in-memory backend for tests and offline planning.
 */
public final class InMemoryMobGoalBackend implements MobGoalBackend {
    private final Map<Mob, List<Entry<?>>> goals = new IdentityHashMap<>();

    @Override
    public synchronized <T extends Mob> void addGoal(@NotNull T mob, int priority, @NotNull Goal<T> goal) {
        this.goals.computeIfAbsent(mob, ignored -> new ArrayList<>()).add(new Entry<>(priority, goal));
        this.goals.get(mob).sort(Comparator.comparingInt(Entry::priority));
    }

    @Override
    public synchronized <T extends Mob> void removeGoal(@NotNull T mob, @NotNull Goal<T> goal) {
        list(mob).removeIf(entry -> entry.goal().equals(goal));
    }

    @Override
    public synchronized <T extends Mob> void removeGoal(@NotNull T mob, @NotNull GoalKey<T> key) {
        list(mob).removeIf(entry -> entry.goal().getKey().equals(key));
    }

    @Override
    public synchronized <T extends Mob> void removeAllGoals(@NotNull T mob) {
        list(mob).clear();
    }

    @Override
    public synchronized <T extends Mob> void removeAllGoals(@NotNull T mob, @NotNull GoalType type) {
        list(mob).removeIf(entry -> entry.goal().getTypes().contains(type));
    }

    @Override
    public synchronized <T extends Mob> boolean hasGoal(@NotNull T mob, @NotNull GoalKey<T> key) {
        return list(mob).stream().anyMatch(entry -> entry.goal().getKey().equals(key));
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public synchronized <T extends Mob> Collection<Goal<T>> allGoals(@NotNull T mob) {
        return list(mob).stream().map(entry -> (Goal<T>) entry.goal()).toList();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public synchronized <T extends Mob> Collection<Goal<T>> runningGoals(@NotNull T mob) {
        return list(mob).stream()
                .map(entry -> (Goal<T>) entry.goal())
                .filter(Goal::shouldStayActive)
                .toList();
    }

    private List<Entry<?>> list(Mob mob) {
        return this.goals.computeIfAbsent(Objects.requireNonNull(mob, "mob"), ignored -> new ArrayList<>());
    }

    private record Entry<T extends Mob>(int priority, Goal<T> goal) {
    }
}
