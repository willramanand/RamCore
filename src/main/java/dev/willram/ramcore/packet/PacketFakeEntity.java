package dev.willram.ramcore.packet;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Packet-only fake entity descriptor. It does not imply server entity state.
 */
public record PacketFakeEntity(
        int entityId,
        @NotNull UUID uuid,
        @NotNull String entityType,
        @NotNull String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        @NotNull Map<String, Object> metadata
) {

    public PacketFakeEntity {
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(worldName, "worldName");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    @NotNull
    public static Builder builder(int entityId, @NotNull UUID uuid, @NotNull String entityType, @NotNull String worldName) {
        return new Builder(entityId, uuid, entityType, worldName);
    }

    public static final class Builder {
        private final int entityId;
        private final UUID uuid;
        private final String entityType;
        private final String worldName;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private Builder(int entityId, UUID uuid, String entityType, String worldName) {
            this.entityId = entityId;
            this.uuid = Objects.requireNonNull(uuid, "uuid");
            this.entityType = Objects.requireNonNull(entityType, "entityType");
            this.worldName = Objects.requireNonNull(worldName, "worldName");
        }

        @NotNull
        public Builder position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @NotNull
        public Builder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }

        @NotNull
        public Builder metadata(@NotNull String key, @NotNull Object value) {
            this.metadata.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        @NotNull
        public PacketFakeEntity build() {
            return new PacketFakeEntity(this.entityId, this.uuid, this.entityType, this.worldName,
                    this.x, this.y, this.z, this.yaw, this.pitch, this.metadata);
        }
    }
}
