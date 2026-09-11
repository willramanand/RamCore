package dev.willram.ramcore.path;

/**
 * Reasons a path task can be cancelled by plugin code.
 */
public enum PathCancelReason {
    MANUAL,
    REPLACED,
    ROUTE_ADVANCED,
    SHUTDOWN
}
