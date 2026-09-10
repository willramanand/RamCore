package dev.willram.ramcore.exception.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Completes an entity-anchored promise exceptionally when the entity was removed before the work
 * could run. On Folia and Paper an entity scheduler silently drops tasks for removed entities;
 * RamCore surfaces that as this exception so chains never hang.
 */
public final class EntityRetiredException extends IllegalStateException {
    private final UUID entityId;

    public EntityRetiredException(@NotNull UUID entityId) {
        super("entity " + entityId + " was removed before the scheduled work could run");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
    }

    @NotNull
    public UUID entityId() {
        return this.entityId;
    }
}
