package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage-neutral claimable loot instance registry.
 */
public interface LootInstanceStore {

    @NotNull
    LootInstance register(@NotNull LootInstance instance);

    @NotNull
    Optional<LootInstance> get(@NotNull UUID instanceId);

    @NotNull
    Collection<LootInstance> instances();

    @NotNull
    LootClaimResult claim(@NotNull UUID instanceId, @NotNull UUID claimantId, @NotNull Instant now);

    boolean remove(@NotNull UUID instanceId);

    @NotNull
    List<LootInstance> sweepExpired(@NotNull Instant now);

    void reroll(@NotNull UUID instanceId, @NotNull List<LootReward> rewards, boolean clearClaims);

    void addListener(@NotNull LootInstanceListener listener);
}
