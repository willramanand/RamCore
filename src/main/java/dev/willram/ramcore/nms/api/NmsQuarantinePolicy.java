package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Default policy for broken or unknown internals on new Minecraft releases.
 */
public final class NmsQuarantinePolicy {

    @NotNull
    public static NmsQuarantinePolicy strict() {
        return new NmsQuarantinePolicy();
    }

    @NotNull
    public NmsQuarantineDecision evaluate(@NotNull NmsCapabilityCheck check) {
        Objects.requireNonNull(check, "check");
        return switch (check.status()) {
            case SUPPORTED -> new NmsQuarantineDecision(check.capability(), NmsQuarantineAction.ALLOW, check.reason());
            case PARTIAL -> new NmsQuarantineDecision(check.capability(), NmsQuarantineAction.WARN,
                    "Partial support: " + check.reason());
            case UNSUPPORTED -> new NmsQuarantineDecision(check.capability(), NmsQuarantineAction.DISABLE,
                    "Disabled unsupported internal: " + check.reason());
            case UNKNOWN -> new NmsQuarantineDecision(check.capability(), NmsQuarantineAction.REQUIRE_REVIEW,
                    "No adapter reported this capability; manual review required before enabling.");
        };
    }

    private NmsQuarantinePolicy() {
    }
}
