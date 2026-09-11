package dev.willram.ramcore.nms.api;

/**
 * Operational decision for an NMS capability.
 */
public enum NmsQuarantineAction {
    ALLOW,
    WARN,
    DISABLE,
    REQUIRE_REVIEW
}
