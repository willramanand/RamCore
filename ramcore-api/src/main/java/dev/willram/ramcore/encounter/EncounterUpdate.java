package dev.willram.ramcore.encounter;

import org.jetbrains.annotations.NotNull;

/**
 * Notification emitted as an encounter changes state.
 */
public record EncounterUpdate(
        @NotNull EncounterInstance encounter,
        @NotNull EncounterSignal signal,
        @NotNull String detail
) {
}
