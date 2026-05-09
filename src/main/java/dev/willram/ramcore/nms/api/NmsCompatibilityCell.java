package dev.willram.ramcore.nms.api;

import dev.willram.ramcore.reflect.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One capability/version entry in the NMS compatibility matrix.
 */
public record NmsCompatibilityCell(
        @NotNull MinecraftVersion minecraftVersion,
        @NotNull NmsCapability capability,
        @NotNull NmsSupportStatus status,
        @NotNull NmsAccessTier tier,
        @NotNull String adapterId,
        @NotNull String reason
) {

    public NmsCompatibilityCell {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(reason, "reason");
    }

    @NotNull
    public String line() {
        return this.minecraftVersion.getVersion() + " | " + this.capability + " | " + this.status
                + " | " + this.tier + " | " + this.adapterId + " | " + this.reason;
    }
}
