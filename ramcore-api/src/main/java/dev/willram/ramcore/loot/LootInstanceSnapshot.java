package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.store.StoreCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The persisted shape of a {@link LootInstance}. Instants are epoch milliseconds and reward
 * payloads are strings produced by a {@link LootPayloadCodec}. Metadata values round-trip through
 * JSON, so numbers come back as doubles.
 */
public record LootInstanceSnapshot(@NotNull UUID id,
                                   @NotNull String tableId,
                                   @NotNull LootInstanceScope scope,
                                   @NotNull LootClaimPolicy claimPolicy,
                                   @NotNull List<RewardSnapshot> rewards,
                                   long createdAtMillis,
                                   @Nullable Long expiresAtMillis,
                                   @NotNull Map<String, Object> metadata,
                                   @NotNull Set<UUID> claimedBy) {

    public LootInstanceSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tableId, "tableId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(claimPolicy, "claimPolicy");
        rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        claimedBy = Set.copyOf(Objects.requireNonNull(claimedBy, "claimedBy"));
    }

    /** One reward with its payload encoded. */
    public record RewardSnapshot(@NotNull String id, @Nullable String payload, int amount, @NotNull Map<String, Object> metadata) {
        public RewardSnapshot {
            Objects.requireNonNull(id, "id");
            metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        }
    }

    @NotNull
    public static LootInstanceSnapshot of(@NotNull LootInstance instance, @NotNull LootPayloadCodec payloads) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(payloads, "payloads");
        List<RewardSnapshot> rewards = new ArrayList<>();
        for (LootReward reward : instance.rewards()) {
            rewards.add(new RewardSnapshot(reward.id(), payloads.encode(reward.payload()), reward.amount(), reward.metadata()));
        }
        return new LootInstanceSnapshot(
                instance.id(),
                instance.tableId().toString(),
                instance.scope(),
                instance.claimPolicy(),
                rewards,
                instance.createdAt().toEpochMilli(),
                instance.expiresAt().map(Instant::toEpochMilli).orElse(null),
                instance.metadata(),
                instance.claimedBy()
        );
    }

    @NotNull
    public LootInstance toInstance(@NotNull LootPayloadCodec payloads) {
        Objects.requireNonNull(payloads, "payloads");
        List<LootReward> rewards = new ArrayList<>();
        for (RewardSnapshot reward : this.rewards) {
            rewards.add(new LootReward(reward.id(), payloads.decode(reward.payload()), reward.amount(), reward.metadata()));
        }
        LootInstance instance = LootInstance.builder(ContentId.parse(this.tableId), rewards)
                .id(this.id)
                .scope(this.scope)
                .claimPolicy(this.claimPolicy)
                .createdAt(Instant.ofEpochMilli(this.createdAtMillis))
                .expiresAt(this.expiresAtMillis == null ? null : Instant.ofEpochMilli(this.expiresAtMillis))
                .metadata(this.metadata)
                .build();
        instance.restoreClaims(this.claimedBy);
        return instance;
    }

    /** Value codec for file and SQL backends. */
    @NotNull
    public static StoreCodec<LootInstanceSnapshot> codec() {
        return StoreCodec.gson(LootInstanceSnapshot.class);
    }
}
