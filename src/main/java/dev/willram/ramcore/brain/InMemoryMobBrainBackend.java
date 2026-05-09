package dev.willram.ramcore.brain;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory memory backend for tests and offline planning.
 */
public final class InMemoryMobBrainBackend implements MobBrainBackend {
    private final Map<LivingEntity, Map<MemoryKey<?>, Object>> memories = new IdentityHashMap<>();

    @Override
    @NotNull
    public synchronized <T> Optional<T> get(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key) {
        Object value = memory(entity).get(Objects.requireNonNull(key, "key"));
        return value == null ? Optional.empty() : Optional.of(key.getMemoryClass().cast(value));
    }

    @Override
    public synchronized <T> void set(@NotNull LivingEntity entity, @NotNull MemoryKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            memory(entity).remove(key);
        } else {
            memory(entity).put(key, key.getMemoryClass().cast(value));
        }
    }

    @Override
    @NotNull
    public synchronized BrainMemorySnapshot snapshot(@NotNull LivingEntity entity, @NotNull Collection<MemoryKey<?>> keys) {
        Map<MemoryKey<?>, Object> memory = memory(entity);
        List<BrainMemoryValue<?>> values = new ArrayList<>();
        Set<MemoryKey<?>> absent = new LinkedHashSet<>();
        for (MemoryKey<?> key : keys) {
            Object value = memory.get(key);
            if (value == null) {
                absent.add(key);
            } else {
                values.add(value(cast(key), value));
            }
        }
        return new BrainMemorySnapshot(values, absent);
    }

    private Map<MemoryKey<?>, Object> memory(LivingEntity entity) {
        return this.memories.computeIfAbsent(Objects.requireNonNull(entity, "entity"), ignored -> new java.util.LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> MemoryKey<T> cast(MemoryKey<?> key) {
        return (MemoryKey<T>) key;
    }

    private static <T> BrainMemoryValue<T> value(MemoryKey<T> key, Object value) {
        return new BrainMemoryValue<>(key, key.getMemoryClass().cast(value));
    }
}
