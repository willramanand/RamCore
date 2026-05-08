package dev.willram.ramcore.encounter;

import org.jetbrains.annotations.NotNull;

/**
 * Hook called when an encounter emits a lifecycle signal.
 */
@FunctionalInterface
public interface EncounterListener {

    void update(@NotNull EncounterUpdate update);
}
