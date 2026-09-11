package dev.willram.ramcore.entity;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Result of spawning a configured entity template.
 */
public record EntitySpawnHandle<T extends LivingEntity>(
        @NotNull T entity,
        @NotNull EntityTemplate<T> template,
        @NotNull List<LivingEntity> passengers
) {
    public EntitySpawnHandle {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(template, "template");
        passengers = List.copyOf(Objects.requireNonNull(passengers, "passengers"));
    }
}
