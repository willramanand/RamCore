package dev.willram.ramcore.stat;

/**
 * How a {@link StatModifier} combines with a stat's running value.
 *
 * <p>All {@link #ADD} modifiers are applied before any {@link #MULTIPLY} modifier, so the order in
 * which sources are collected does not affect the result.</p>
 */
public enum StatOperation {

    /** Adds the modifier amount to the base sum. */
    ADD,

    /** Scales the additive sum. An amount of {@code 0.10} means "+10%". */
    MULTIPLY
}
