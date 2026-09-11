package dev.willram.ramcore.objective;

import org.jetbrains.annotations.NotNull;

/**
 * Hook called when objective progress changes.
 */
@FunctionalInterface
public interface ObjectiveProgressListener {

    void progress(@NotNull ObjectiveUpdate update);
}
