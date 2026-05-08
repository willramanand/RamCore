package dev.willram.ramcore.objective;

import org.jetbrains.annotations.NotNull;

/**
 * Public progress view for one objective task.
 */
public record ObjectiveTaskProgress(
        @NotNull String taskId,
        long current,
        long required,
        boolean completed,
        boolean hidden
) {
}
