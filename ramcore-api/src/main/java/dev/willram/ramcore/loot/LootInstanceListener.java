package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Listener hook for loot instance lifecycle events.
 */
public interface LootInstanceListener {
    default void generated(@NotNull LootInstance instance) {
    }

    default void claimed(@NotNull LootInstance instance, @NotNull LootClaimResult result) {
    }

    default void expired(@NotNull LootInstance instance) {
    }

    default void rerolled(@NotNull LootInstance instance, @NotNull List<LootReward> previousRewards) {
    }
}
