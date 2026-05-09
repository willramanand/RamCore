package dev.willram.ramcore.brain;

import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One present memory value.
 */
public record BrainMemoryValue<T>(
        @NotNull MemoryKey<T> key,
        @NotNull T value
) {
    public BrainMemoryValue {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }
}
