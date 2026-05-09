package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Decision for whether an internal capability should be used.
 */
public record NmsQuarantineDecision(
        @NotNull NmsCapability capability,
        @NotNull NmsQuarantineAction action,
        @NotNull String reason
) {

    public NmsQuarantineDecision {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(reason, "reason");
    }

    public boolean usable() {
        return this.action == NmsQuarantineAction.ALLOW || this.action == NmsQuarantineAction.WARN;
    }
}
