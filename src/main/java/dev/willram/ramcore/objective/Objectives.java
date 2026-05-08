package dev.willram.ramcore.objective;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

/**
 * Entry points for RamCore objective helpers.
 */
public final class Objectives {

    @NotNull
    public static ObjectiveTracker tracker() {
        return new ObjectiveTracker();
    }

    @NotNull
    public static ObjectiveDefinition.Builder objective(@NotNull ContentId id) {
        return ObjectiveDefinition.builder(id);
    }

    @NotNull
    public static ObjectiveTask task(@NotNull String id, @NotNull ObjectiveAction action, @NotNull String target, long required) {
        return ObjectiveTask.of(id, action, target, required);
    }

    private Objectives() {
    }
}
