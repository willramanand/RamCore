package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Context used while generating loot.
 */
public record LootContext(
        @NotNull String scope,
        @Nullable UUID playerId,
        @Nullable UUID groupId,
        @Nullable UUID sourceEntityId,
        @Nullable String regionId,
        @Nullable String worldName,
        double luck,
        @NotNull Map<String, Object> metadata
) {

    @NotNull
    public static LootContext of(@NotNull String scope) {
        return builder(scope).build();
    }

    @NotNull
    public static Builder builder(@NotNull String scope) {
        return new Builder(scope);
    }

    public LootContext {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(metadata, "metadata");
        if (scope.isBlank()) {
            throw new IllegalArgumentException("loot context scope must not be blank");
        }
        metadata = Map.copyOf(metadata);
    }

    @NotNull
    public Optional<Object> metadata(@NotNull String key) {
        return Optional.ofNullable(this.metadata.get(Objects.requireNonNull(key, "key")));
    }

    public static final class Builder {
        private final String scope;
        private UUID playerId;
        private UUID groupId;
        private UUID sourceEntityId;
        private String regionId;
        private String worldName;
        private double luck;
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private Builder(String scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
        }

        @NotNull
        public Builder player(@Nullable UUID playerId) {
            this.playerId = playerId;
            return this;
        }

        @NotNull
        public Builder group(@Nullable UUID groupId) {
            this.groupId = groupId;
            return this;
        }

        @NotNull
        public Builder sourceEntity(@Nullable UUID sourceEntityId) {
            this.sourceEntityId = sourceEntityId;
            return this;
        }

        @NotNull
        public Builder region(@Nullable String regionId) {
            this.regionId = regionId;
            return this;
        }

        @NotNull
        public Builder world(@Nullable String worldName) {
            this.worldName = worldName;
            return this;
        }

        @NotNull
        public Builder luck(double luck) {
            this.luck = luck;
            return this;
        }

        @NotNull
        public Builder metadata(@NotNull String key, @NotNull Object value) {
            this.metadata.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        @NotNull
        public Builder metadata(@NotNull Map<String, Object> metadata) {
            this.metadata.putAll(Objects.requireNonNull(metadata, "metadata"));
            return this;
        }

        @NotNull
        public LootContext build() {
            return new LootContext(this.scope, this.playerId, this.groupId, this.sourceEntityId, this.regionId, this.worldName, this.luck, this.metadata);
        }
    }
}
