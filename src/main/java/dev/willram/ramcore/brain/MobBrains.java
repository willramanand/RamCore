package dev.willram.ramcore.brain;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Facade for Paper-exposed brain memory access.
 */
public final class MobBrains {
    public static final List<MemoryKey<?>> COMMON_DEBUG_KEYS = List.of(
            MemoryKey.ANGRY_AT,
            MemoryKey.ATTACK_COOLING_DOWN,
            MemoryKey.ATTACK_TARGET_COOLDOWN,
            MemoryKey.CANT_REACH_WALK_TARGET_SINCE,
            MemoryKey.HOME,
            MemoryKey.JOB_SITE,
            MemoryKey.MEETING_POINT,
            MemoryKey.IS_PANICKING,
            MemoryKey.ITEM_PICKUP_COOLDOWN_TICKS
    );

    @NotNull
    public static MobBrainBackend paperBackend() {
        return new PaperMobBrainBackend();
    }

    @NotNull
    public static MobBrainBackend memoryBackend() {
        return new InMemoryMobBrainBackend();
    }

    @NotNull
    public static MobBrainController controller(@NotNull LivingEntity entity) {
        return controller(entity, paperBackend());
    }

    @NotNull
    public static MobBrainController controller(@NotNull LivingEntity entity, @NotNull MobBrainBackend backend) {
        return new MobBrainController(entity, backend);
    }

    @NotNull
    public static NmsCapabilityCheck memoryCapability() {
        return NmsCapabilityCheck.partial(
                NmsCapability.BRAIN_MEMORY,
                NmsAccessTier.PAPER_API,
                "paper-memory-keys",
                "Paper exposes typed LivingEntity memory get/set for selected MemoryKey values."
        );
    }

    @NotNull
    public static NmsCapabilityCheck sensorCapability() {
        return NmsCapabilityCheck.unsupported(
                NmsCapability.BRAIN_SENSORS,
                NmsAccessTier.VERSIONED_IMPLEMENTATION,
                "none",
                "Paper does not expose stable brain sensor inspection; add a versioned adapter before use."
        );
    }

    @NotNull
    public static NmsCapabilityCheck activityCapability() {
        return NmsCapabilityCheck.unsupported(
                NmsCapability.BRAIN_ACTIVITY,
                NmsAccessTier.VERSIONED_IMPLEMENTATION,
                "none",
                "Paper does not expose stable brain activity inspection or mutation; add a versioned adapter before use."
        );
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapabilities(@NotNull NmsAccessRegistry registry) {
        return registry.override(memoryCapability())
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.BRAIN_MEMORY_READ,
                        NmsAccessTier.PAPER_API,
                        "paper-memory-keys",
                        "Paper exposes LivingEntity#getMemory for selected MemoryKey values."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.BRAIN_MEMORY_WRITE,
                        NmsAccessTier.PAPER_API,
                        "paper-memory-keys",
                        "Paper exposes LivingEntity#setMemory for selected MemoryKey values."
                ))
                .override(sensorCapability())
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.BRAIN_SENSOR_READ,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Paper does not expose stable brain sensor state; add a versioned adapter before use."
                ))
                .override(activityCapability())
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.BRAIN_ACTIVITY_READ,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Paper does not expose stable current-activity inspection; add a versioned adapter before use."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.BRAIN_ACTIVITY_WRITE,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Paper does not expose stable activity forcing/clearing; add a versioned adapter before use."
                ));
    }

    private MobBrains() {
    }
}
