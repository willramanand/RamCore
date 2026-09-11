package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Generated loot value. The payload is intentionally generic so consuming plugins can render it as
 * an item, command, permission, currency grant, token, or custom reward object.
 */
public record LootReward(
        @NotNull String id,
        @Nullable Object payload,
        int amount,
        @NotNull Map<String, Object> metadata
) {

    @NotNull
    public static LootReward of(@NotNull String id) {
        return of(id, null, 1);
    }

    @NotNull
    public static LootReward of(@NotNull String id, @Nullable Object payload) {
        return of(id, payload, 1);
    }

    @NotNull
    public static LootReward of(@NotNull String id, @Nullable Object payload, int amount) {
        return new LootReward(id, payload, amount, Map.of());
    }

    public LootReward {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(metadata, "metadata");
        if (id.isBlank()) {
            throw new IllegalArgumentException("loot reward id must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("loot reward amount must be > 0");
        }
        metadata = Map.copyOf(metadata);
    }

    @NotNull
    public LootReward withMetadata(@NotNull Map<String, Object> metadata) {
        return new LootReward(this.id, this.payload, this.amount, metadata);
    }
}
