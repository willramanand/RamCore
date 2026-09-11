package dev.willram.ramcore.brain;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Bukkit/Paper memory-key backend.
 */
public final class PaperMobBrainBackend implements MobBrainBackend {

    @Override
    @NotNull
    public <T> Optional<T> get(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(entity.getMemory(key));
    }

    @Override
    public <T> void set(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key, T value) {
        Objects.requireNonNull(entity, "entity").setMemory(Objects.requireNonNull(key, "key"), value);
    }

    @Override
    @NotNull
    public BrainMemorySnapshot snapshot(@NotNull LivingEntity entity, @NotNull Collection<MemoryKey<?>> keys) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(keys, "keys");
        List<BrainMemoryValue<?>> values = new ArrayList<>();
        Set<MemoryKey<?>> absent = new LinkedHashSet<>();
        for (MemoryKey<?> key : keys) {
            Object value = entity.getMemory(cast(key));
            if (value == null) {
                absent.add(key);
            } else {
                values.add(value(cast(key), value));
            }
        }
        return new BrainMemorySnapshot(values, absent);
    }

    @SuppressWarnings("unchecked")
    private static <T> MemoryKey<T> cast(MemoryKey<?> key) {
        return (MemoryKey<T>) key;
    }

    private static <T> BrainMemoryValue<T> value(MemoryKey<T> key, Object value) {
        return new BrainMemoryValue<>(key, key.getMemoryClass().cast(value));
    }
}
