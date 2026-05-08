package dev.willram.ramcore.encounter;

import org.jetbrains.annotations.NotNull;

/**
 * Executed when a timed encounter ability is due.
 */
@FunctionalInterface
public interface EncounterAbilityAction {

    void execute(@NotNull EncounterInstance encounter, @NotNull EncounterAbility ability);
}
