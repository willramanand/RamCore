package dev.willram.ramcore.nms.api;

import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of NMS support decisions for startup/admin diagnostics.
 */
public record NmsDiagnostics(
        @NotNull MinecraftVersion minecraftVersion,
        @NotNull NmsVersion nmsVersion,
        @NotNull List<String> adapterIds,
        @NotNull List<NmsCapabilityCheck> capabilityChecks
) {
    public NmsDiagnostics {
        adapterIds = List.copyOf(adapterIds);
        capabilityChecks = List.copyOf(capabilityChecks);
    }

    @NotNull
    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("Minecraft version: " + this.minecraftVersion.getVersion());
        lines.add("NMS version: " + this.nmsVersion.name());
        lines.add("Adapters: " + (this.adapterIds.isEmpty() ? "none" : String.join(", ", this.adapterIds)));
        for (NmsCapabilityCheck check : this.capabilityChecks) {
            lines.add(check.summary());
        }
        return List.copyOf(lines);
    }
}
