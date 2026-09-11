package dev.willram.ramcore.loot;

/**
 * Outcome of attempting to claim a loot instance.
 */
public enum LootClaimStatus {
    SUCCESS,
    ALREADY_CLAIMED,
    EXPIRED,
    NOT_FOUND
}
