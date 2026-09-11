package dev.willram.ramcore.nms.api;

import dev.willram.ramcore.reflect.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One capability check result for diagnostics and fail-fast guards.
 */
public record NmsCapabilityCheck(
        @NotNull NmsCapability capability,
        @NotNull NmsSupportStatus status,
        @NotNull NmsAccessTier tier,
        @NotNull String adapterId,
        @Nullable MinecraftVersion minimumVersion,
        @Nullable MinecraftVersion maximumVersion,
        @NotNull String reason
) {

    @NotNull
    public static NmsCapabilityCheck supported(@NotNull NmsCapability capability, @NotNull NmsAccessTier tier,
                                               @NotNull String adapterId, @NotNull String reason) {
        return new NmsCapabilityCheck(capability, NmsSupportStatus.SUPPORTED, tier, adapterId, null, null, reason);
    }

    @NotNull
    public static NmsCapabilityCheck partial(@NotNull NmsCapability capability, @NotNull NmsAccessTier tier,
                                             @NotNull String adapterId, @NotNull String reason) {
        return new NmsCapabilityCheck(capability, NmsSupportStatus.PARTIAL, tier, adapterId, null, null, reason);
    }

    @NotNull
    public static NmsCapabilityCheck unsupported(@NotNull NmsCapability capability, @NotNull String adapterId, @NotNull String reason) {
        return new NmsCapabilityCheck(capability, NmsSupportStatus.UNSUPPORTED, NmsAccessTier.PAPER_API, adapterId, null, null, reason);
    }

    @NotNull
    public static NmsCapabilityCheck unsupported(@NotNull NmsCapability capability, @NotNull NmsAccessTier tier,
                                                 @NotNull String adapterId, @NotNull String reason) {
        return new NmsCapabilityCheck(capability, NmsSupportStatus.UNSUPPORTED, tier, adapterId, null, null, reason);
    }

    @NotNull
    public static NmsCapabilityCheck unknown(@NotNull NmsCapability capability) {
        return new NmsCapabilityCheck(capability, NmsSupportStatus.UNKNOWN, NmsAccessTier.PAPER_API, "none", null, null, "No adapter reported this capability.");
    }

    public NmsCapabilityCheck {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(reason, "reason");
    }

    public boolean usable() {
        return this.status.usable();
    }

    @NotNull
    public String summary() {
        return this.capability + "=" + this.status + " [" + this.capability.category().key() + "] via "
                + this.adapterId + " (" + this.tier + "): " + this.reason;
    }
}
