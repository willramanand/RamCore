package dev.willram.ramcore.brain;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Backend boundary for brain memory access.
 */
public interface MobBrainBackend {

    @NotNull
    <T> Optional<T> get(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key);

    <T> void set(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key, T value);

    default <T> void clear(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key) {
        set(entity, key, null);
    }

    @NotNull
    BrainMemorySnapshot snapshot(@NotNull LivingEntity entity, @NotNull Collection<MemoryKey<?>> keys);
}
