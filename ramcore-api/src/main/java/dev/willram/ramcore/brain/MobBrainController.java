package dev.willram.ramcore.brain;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Typed controller for one living entity's exposed brain memories.
 */
public final class MobBrainController {
    private final LivingEntity entity;
    private final MobBrainBackend backend;

    MobBrainController(@NotNull LivingEntity entity, @NotNull MobBrainBackend backend) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @NotNull
    public LivingEntity entity() {
        return this.entity;
    }

    @NotNull
    public <T> Optional<T> get(@NotNull MemoryKey<T> key) {
        return this.backend.get(this.entity, key);
    }

    public <T> boolean has(@NotNull MemoryKey<T> key) {
        return get(key).isPresent();
    }

    @NotNull
    public <T> MobBrainController set(@NotNull MemoryKey<T> key, T value) {
        this.backend.set(this.entity, key, value);
        return this;
    }

    @NotNull
    public <T> MobBrainController clear(@NotNull MemoryKey<T> key) {
        this.backend.clear(this.entity, key);
        return this;
    }

    @NotNull
    public MobBrainController angryAt(@NotNull UUID targetId) {
        return set(MemoryKey.ANGRY_AT, targetId);
    }

    @NotNull
    public MobBrainController home(@NotNull Location location) {
        return set(MemoryKey.HOME, location);
    }

    @NotNull
    public MobBrainController meetingPoint(@NotNull Location location) {
        return set(MemoryKey.MEETING_POINT, location);
    }

    @NotNull
    public MobBrainController jobSite(@NotNull Location location) {
        return set(MemoryKey.JOB_SITE, location);
    }

    @NotNull
    public MobBrainController attackCooldown(int ticks) {
        return set(MemoryKey.ATTACK_TARGET_COOLDOWN, ticks);
    }

    @NotNull
    public BrainMemorySnapshot snapshot(@NotNull Collection<MemoryKey<?>> keys) {
        return this.backend.snapshot(this.entity, keys);
    }

    @NotNull
    public MobBrainDiagnostics diagnostics(@NotNull Collection<MemoryKey<?>> keys) {
        return MobBrainDiagnostics.of(
                this.entity,
                snapshot(keys),
                MobBrains.memoryCapability(),
                MobBrains.sensorCapability(),
                MobBrains.activityCapability()
        );
    }
}
