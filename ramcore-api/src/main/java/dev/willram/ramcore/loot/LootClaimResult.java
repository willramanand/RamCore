package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Result of claiming an instance.
 */
public record LootClaimResult(
        @NotNull LootClaimStatus status,
        @NotNull UUID instanceId,
        @NotNull UUID claimantId,
        @NotNull List<LootReward> rewards
) {
    public LootClaimResult {
        rewards = List.copyOf(rewards);
    }

    @NotNull
    public static LootClaimResult notFound(@NotNull UUID instanceId, @NotNull UUID claimantId) {
        return new LootClaimResult(LootClaimStatus.NOT_FOUND, instanceId, claimantId, List.of());
    }

    public boolean successful() {
        return this.status == LootClaimStatus.SUCCESS;
    }
}
