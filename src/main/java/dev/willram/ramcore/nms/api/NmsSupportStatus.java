package dev.willram.ramcore.nms.api;

/**
 * Support status for a capability on the active server version.
 */
public enum NmsSupportStatus {
    SUPPORTED,
    PARTIAL,
    UNSUPPORTED,
    UNKNOWN;

    public boolean usable() {
        return this == SUPPORTED || this == PARTIAL;
    }
}
