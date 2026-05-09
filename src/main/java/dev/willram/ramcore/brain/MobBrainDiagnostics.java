package dev.willram.ramcore.brain;

import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Read-only brain diagnostics snapshot.
 */
public record MobBrainDiagnostics(
        @NotNull UUID entityId,
        @NotNull BrainMemorySnapshot memories,
        @NotNull NmsCapabilityCheck memoryCapability,
        @NotNull NmsCapabilityCheck sensorCapability,
        @NotNull NmsCapabilityCheck activityCapability
) {
    @NotNull
    public static MobBrainDiagnostics of(@NotNull LivingEntity entity, @NotNull BrainMemorySnapshot memories,
                                         @NotNull NmsCapabilityCheck memoryCapability,
                                         @NotNull NmsCapabilityCheck sensorCapability,
                                         @NotNull NmsCapabilityCheck activityCapability) {
        return new MobBrainDiagnostics(entity.getUniqueId(), memories, memoryCapability, sensorCapability, activityCapability);
    }

    @NotNull
    public List<String> lines() {
        return List.of(
                "Entity: " + this.entityId,
                "Present memories: " + this.memories.presentKeys(),
                "Missing memories: " + this.memories.absent().stream().map(key -> key.getKey().toString()).toList(),
                this.memoryCapability.summary(),
                this.sensorCapability.summary(),
                this.activityCapability.summary()
        );
    }
}
