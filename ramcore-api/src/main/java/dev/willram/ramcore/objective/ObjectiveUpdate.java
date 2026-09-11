package dev.willram.ramcore.objective;

import org.jetbrains.annotations.NotNull;

/**
 * Result emitted after an objective task advances.
 */
public record ObjectiveUpdate(
        @NotNull ObjectiveDefinition definition,
        @NotNull ObjectiveSubject subject,
        @NotNull ObjectiveTask task,
        long before,
        long after,
        boolean taskCompleted,
        boolean objectiveCompleted,
        @NotNull ObjectiveEvent event
) {
}
