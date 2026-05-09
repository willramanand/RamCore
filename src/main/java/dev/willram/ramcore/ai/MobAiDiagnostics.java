package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot of goal state useful for admin/debug output.
 */
public record MobAiDiagnostics<T extends Mob>(
        @NotNull UUID mobId,
        @NotNull List<GoalKey<T>> allGoals,
        @NotNull List<GoalKey<T>> runningGoals,
        @NotNull List<MobGoalRegistration<T>> trackedGoals,
        @NotNull List<MobGoalConflict<T>> conflicts,
        @NotNull Optional<UUID> targetId
) {
    public static <T extends Mob> MobAiDiagnostics<T> of(
            @NotNull T mob,
            @NotNull List<GoalKey<T>> allGoals,
            @NotNull List<GoalKey<T>> runningGoals,
            @NotNull List<MobGoalRegistration<T>> trackedGoals,
            @NotNull List<MobGoalConflict<T>> conflicts
    ) {
        LivingEntity target = mob.getTarget();
        return new MobAiDiagnostics<>(
                mob.getUniqueId(),
                List.copyOf(allGoals),
                List.copyOf(runningGoals),
                List.copyOf(trackedGoals),
                List.copyOf(conflicts),
                target == null ? Optional.empty() : Optional.of(target.getUniqueId())
        );
    }
}
