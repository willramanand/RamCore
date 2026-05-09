package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claimable generated loot.
 */
public final class LootInstance {
    private final UUID id;
    private final ContentId tableId;
    private final LootInstanceScope scope;
    private final LootClaimPolicy claimPolicy;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Map<String, Object> metadata;
    private final Set<UUID> claimedBy = ConcurrentHashMap.newKeySet();
    private List<LootReward> rewards;

    private LootInstance(@NotNull UUID id, @NotNull ContentId tableId, @NotNull LootInstanceScope scope,
                         @NotNull LootClaimPolicy claimPolicy, @NotNull List<LootReward> rewards,
                         @NotNull Instant createdAt, @Nullable Instant expiresAt, @NotNull Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "id");
        this.tableId = Objects.requireNonNull(tableId, "tableId");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.claimPolicy = Objects.requireNonNull(claimPolicy, "claimPolicy");
        this.rewards = List.copyOf(rewards);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = expiresAt;
        this.metadata = Map.copyOf(metadata);
    }

    @NotNull
    public static Builder builder(@NotNull ContentId tableId, @NotNull List<LootReward> rewards) {
        return new Builder(tableId, rewards);
    }

    @NotNull
    public UUID id() {
        return this.id;
    }

    @NotNull
    public ContentId tableId() {
        return this.tableId;
    }

    @NotNull
    public LootInstanceScope scope() {
        return this.scope;
    }

    @NotNull
    public LootClaimPolicy claimPolicy() {
        return this.claimPolicy;
    }

    @NotNull
    public List<LootReward> rewards() {
        return this.rewards;
    }

    @NotNull
    public Instant createdAt() {
        return this.createdAt;
    }

    @NotNull
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(this.expiresAt);
    }

    @NotNull
    public Map<String, Object> metadata() {
        return this.metadata;
    }

    @NotNull
    public Set<UUID> claimedBy() {
        return Set.copyOf(this.claimedBy);
    }

    public boolean expired(@NotNull Instant now) {
        return this.expiresAt != null && !now.isBefore(this.expiresAt);
    }

    public synchronized LootClaimResult claim(@NotNull UUID claimantId, @NotNull Instant now) {
        Objects.requireNonNull(claimantId, "claimantId");
        Objects.requireNonNull(now, "now");
        if (expired(now)) {
            return new LootClaimResult(LootClaimStatus.EXPIRED, this.id, claimantId, List.of());
        }
        if (this.claimPolicy == LootClaimPolicy.SINGLE_CLAIM && !this.claimedBy.isEmpty()) {
            return new LootClaimResult(LootClaimStatus.ALREADY_CLAIMED, this.id, claimantId, List.of());
        }
        if (this.claimPolicy == LootClaimPolicy.PER_PLAYER_ONCE && this.claimedBy.contains(claimantId)) {
            return new LootClaimResult(LootClaimStatus.ALREADY_CLAIMED, this.id, claimantId, List.of());
        }

        this.claimedBy.add(claimantId);
        return new LootClaimResult(LootClaimStatus.SUCCESS, this.id, claimantId, this.rewards);
    }

    public synchronized void reroll(@NotNull List<LootReward> rewards, boolean clearClaims) {
        this.rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
        if (clearClaims) {
            this.claimedBy.clear();
        }
    }

    public static final class Builder {
        private final ContentId tableId;
        private final List<LootReward> rewards;
        private UUID id = UUID.randomUUID();
        private LootInstanceScope scope = LootInstanceScope.PERSONAL;
        private LootClaimPolicy claimPolicy = LootClaimPolicy.PER_PLAYER_ONCE;
        private Instant createdAt = Instant.now();
        private Instant expiresAt;
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private Builder(ContentId tableId, List<LootReward> rewards) {
            this.tableId = Objects.requireNonNull(tableId, "tableId");
            this.rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
        }

        @NotNull
        public Builder id(@NotNull UUID id) {
            this.id = Objects.requireNonNull(id, "id");
            return this;
        }

        @NotNull
        public Builder scope(@NotNull LootInstanceScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
            return this;
        }

        @NotNull
        public Builder claimPolicy(@NotNull LootClaimPolicy claimPolicy) {
            this.claimPolicy = Objects.requireNonNull(claimPolicy, "claimPolicy");
            return this;
        }

        @NotNull
        public Builder createdAt(@NotNull Instant createdAt) {
            this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
            return this;
        }

        @NotNull
        public Builder expiresAt(@Nullable Instant expiresAt) {
            this.expiresAt = expiresAt;
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
        public LootInstance build() {
            return new LootInstance(this.id, this.tableId, this.scope, this.claimPolicy, this.rewards, this.createdAt, this.expiresAt, this.metadata);
        }
    }
}
