package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Diagnostic conflict between tracked goals.
 */
public record MobGoalConflict<T extends Mob>(
        int priority,
        @NotNull GoalType type,
        @NotNull GoalKey<T> first,
        @NotNull GoalKey<T> second
) {
}
