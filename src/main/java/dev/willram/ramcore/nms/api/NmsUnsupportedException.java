package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when plugin code requests an internal capability that is not available.
 */
public final class NmsUnsupportedException extends RuntimeException {
    private final NmsCapability capability;
    private final NmsCapabilityCheck check;

    public NmsUnsupportedException(@NotNull NmsCapability capability, @NotNull NmsCapabilityCheck check) {
        super("RamCore NMS capability unsupported: " + capability + ". " + check.reason());
        this.capability = capability;
        this.check = check;
    }

    @NotNull
    public NmsCapability capability() {
        return this.capability;
    }

    @NotNull
    public NmsCapabilityCheck check() {
        return this.check;
    }
}
