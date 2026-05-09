package dev.willram.ramcore.path;

/**
 * Runtime state for a managed path task.
 */
public enum PathStatus {
    PENDING,
    RUNNING,
    COMPLETE,
    FAILED,
    CANCELLED
}
