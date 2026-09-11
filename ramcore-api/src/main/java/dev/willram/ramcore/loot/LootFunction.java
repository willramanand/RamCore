package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Transforms a generated loot reward.
 */
@FunctionalInterface
public interface LootFunction {

    @NotNull
    LootReward apply(@NotNull LootReward reward, @NotNull LootContext context, @NotNull Random random);
}
