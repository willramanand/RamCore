package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent goal controller for one mob.
 */
public final class MobAiController<T extends Mob> {
    private final T mob;
    private final MobGoalBackend backend;
    private final Map<GoalKey<T>, MobGoalRegistration<T>> tracked = new LinkedHashMap<>();

    MobAiController(@NotNull T mob, @NotNull MobGoalBackend backend) {
        this.mob = Objects.requireNonNull(mob, "mob");
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @NotNull
    public T mob() {
        return this.mob;
    }

    @NotNull
    public MobAiController<T> add(int priority, @NotNull Goal<T> goal) {
        Objects.requireNonNull(goal, "goal");
        this.backend.addGoal(this.mob, priority, goal);
        this.tracked.put(goal.getKey(), new MobGoalRegistration<>(priority, goal, false));
        return this;
    }

    @NotNull
    public MobAiController<T> replace(int priority, @NotNull Goal<T> goal) {
        remove(goal.getKey());
        return add(priority, goal);
    }

    @NotNull
    public MobAiController<T> remove(@NotNull GoalKey<T> key) {
        Objects.requireNonNull(key, "key");
        this.backend.removeGoal(this.mob, key);
        this.tracked.remove(key);
        return this;
    }

    @NotNull
    public MobAiController<T> remove(@NotNull Goal<T> goal) {
        Objects.requireNonNull(goal, "goal");
        this.backend.removeGoal(this.mob, goal);
        this.tracked.remove(goal.getKey());
        return this;
    }

    @NotNull
    public MobAiController<T> removeAll() {
        this.backend.removeAllGoals(this.mob);
        this.tracked.clear();
        return this;
    }

    @NotNull
    public MobAiController<T> removeType(@NotNull GoalType type) {
        this.backend.removeAllGoals(this.mob, type);
        this.tracked.entrySet().removeIf(entry -> entry.getValue().goal().getTypes().contains(type));
        return this;
    }

    @NotNull
    public MobAiController<T> pause(@NotNull GoalKey<T> key) {
        MobGoalRegistration<T> registration = this.tracked.get(Objects.requireNonNull(key, "key"));
        if (registration == null || registration.paused()) {
            return this;
        }
        this.backend.removeGoal(this.mob, key);
        this.tracked.put(key, registration.paused(true));
        return this;
    }

    @NotNull
    public MobAiController<T> resume(@NotNull GoalKey<T> key) {
        MobGoalRegistration<T> registration = this.tracked.get(Objects.requireNonNull(key, "key"));
        if (registration == null || !registration.paused()) {
            return this;
        }
        this.backend.addGoal(this.mob, registration.priority(), registration.goal());
        this.tracked.put(key, registration.paused(false));
        return this;
    }

    @NotNull
    public MobAiController<T> restore(@NotNull MobGoalSnapshot<T> snapshot) {
        removeAll();
        for (MobGoalRegistration<T> registration : snapshot.registrations()) {
            this.tracked.put(registration.key(), registration);
            if (!registration.paused()) {
                this.backend.addGoal(this.mob, registration.priority(), registration.goal());
            }
        }
        return this;
    }

    @NotNull
    public MobGoalSnapshot<T> snapshot() {
        return new MobGoalSnapshot<>(List.copyOf(this.tracked.values()));
    }

    public boolean has(@NotNull GoalKey<T> key) {
        return this.backend.hasGoal(this.mob, key);
    }

    @NotNull
    public List<MobGoalRegistration<T>> trackedGoals() {
        return List.copyOf(this.tracked.values());
    }

    @NotNull
    public MobAiDiagnostics<T> diagnostics() {
        List<GoalKey<T>> all = this.backend.allGoals(this.mob).stream().map(Goal::getKey).toList();
        List<GoalKey<T>> running = this.backend.runningGoals(this.mob).stream().map(Goal::getKey).toList();
        return MobAiDiagnostics.of(this.mob, all, running, trackedGoals(), conflicts());
    }

    @NotNull
    public List<MobGoalConflict<T>> conflicts() {
        List<MobGoalRegistration<T>> registrations = trackedGoals().stream()
                .filter(registration -> !registration.paused())
                .toList();
        List<MobGoalConflict<T>> conflicts = new ArrayList<>();
        for (int i = 0; i < registrations.size(); i++) {
            MobGoalRegistration<T> left = registrations.get(i);
            for (int j = i + 1; j < registrations.size(); j++) {
                MobGoalRegistration<T> right = registrations.get(j);
                if (left.priority() != right.priority()) {
                    continue;
                }
                EnumSet<GoalType> shared = EnumSet.copyOf(left.goal().getTypes());
                shared.retainAll(right.goal().getTypes());
                for (GoalType type : shared) {
                    conflicts.add(new MobGoalConflict<>(left.priority(), type, left.key(), right.key()));
                }
            }
        }
        return List.copyOf(conflicts);
    }
}
