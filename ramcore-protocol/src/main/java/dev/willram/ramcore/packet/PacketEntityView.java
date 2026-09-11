package dev.willram.ramcore.packet;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity identity used in packet visual operations.
 */
public record PacketEntityView(int entityId, @NotNull UUID uuid) {

    public PacketEntityView {
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
        Objects.requireNonNull(uuid, "uuid");
    }

    @NotNull
    public static PacketEntityView of(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return new PacketEntityView(entity.getEntityId(), entity.getUniqueId());
    }
}
