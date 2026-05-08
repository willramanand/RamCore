package dev.willram.ramcore.encounter;

/**
 * Encounter lifecycle signal emitted to listeners.
 */
public enum EncounterSignal {
    START,
    TICK,
    DAMAGE,
    PHASE_CHANGE,
    ABILITY,
    ENRAGE,
    WIPE,
    RESET,
    COMPLETE
}
