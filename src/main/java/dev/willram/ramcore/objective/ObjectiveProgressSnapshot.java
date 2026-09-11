package dev.willram.ramcore.objective;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * The persisted shape of one subject's progress on one objective: task id to amount.
 *
 * @param amounts task id to current amount
 */
public record ObjectiveProgressSnapshot(@NotNull Map<String, Long> amounts) {

    public ObjectiveProgressSnapshot {
        amounts = Map.copyOf(Objects.requireNonNull(amounts, "amounts"));
    }

    @NotNull
    public static ObjectiveProgressSnapshot of(@NotNull ObjectiveProgress progress) {
        return new ObjectiveProgressSnapshot(Objects.requireNonNull(progress, "progress").snapshot());
    }
}
