package dev.willram.ramcore.brain;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Snapshot of selected entity memories.
 */
public record BrainMemorySnapshot(
        @NotNull List<BrainMemoryValue<?>> values,
        @NotNull Set<MemoryKey<?>> absent
) {
    public BrainMemorySnapshot {
        values = List.copyOf(values);
        absent = Set.copyOf(absent);
    }

    @NotNull
    public <T> Optional<T> get(@NotNull MemoryKey<T> key) {
        Objects.requireNonNull(key, "key");
        for (BrainMemoryValue<?> value : this.values) {
            if (value.key().equals(key)) {
                return Optional.of(key.getMemoryClass().cast(value.value()));
            }
        }
        return Optional.empty();
    }

    public boolean has(@NotNull MemoryKey<?> key) {
        return getUntyped(key).isPresent();
    }

    @NotNull
    public Optional<Object> getUntyped(@NotNull MemoryKey<?> key) {
        Objects.requireNonNull(key, "key");
        for (BrainMemoryValue<?> value : this.values) {
            if (value.key().equals(key)) {
                return Optional.of(value.value());
            }
        }
        return Optional.empty();
    }

    @NotNull
    public Set<NamespacedKey> presentKeys() {
        return this.values.stream().map(value -> value.key().getKey()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
