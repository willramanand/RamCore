package dev.willram.ramcore.path;

/**
 * Reasons a path task can fail before completion.
 */
public enum PathFailureReason {
    NO_PATH,
    MOVE_REJECTED,
    TOO_FAR,
    STUCK,
    TIMEOUT,
    TARGET_UNAVAILABLE,
    ENTITY_RETIRED
}
