package dev.willram.ramcore.ability;

/**
 * The outcome of an {@link AbilityCaster#cast} attempt.
 */
public enum AbilityCastStatus {

    /** Instant ability executed immediately. */
    CAST,

    /** Channelled ability started; it executes when its cast time elapses. */
    CASTING,

    /** Denied: the ability is still on cooldown. */
    ON_COOLDOWN,

    /** Denied: the caster does not meet the ability's {@link StatCost}. */
    INSUFFICIENT_COST,

    /** Denied: the action's {@code validate} returned errors. */
    INVALID,

    /** Denied: the caster is already channelling another ability. */
    BUSY;

    /** Whether the cast started (instant executed or channel began). */
    public boolean started() {
        return this == CAST || this == CASTING;
    }
}
