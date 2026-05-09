package dev.willram.ramcore.loot;

import dev.willram.ramcore.random.RandomSelector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Rolls loot tables without applying side effects.
 */
public final class LootGenerator {

    @NotNull
    public LootGenerationResult generate(@NotNull LootTable table, @NotNull LootContext context, @NotNull Random random) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(random, "random");

        List<LootReward> rewards = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LootEntry entry : table.guaranteed()) {
            if (entry.condition().test(context)) {
                addReward(rewards, errors, entry, context);
            }
        }

        List<LootEntry> weighted = table.weighted().stream()
                .filter(entry -> entry.condition().test(context))
                .toList();
        int totalRolls = table.rolls() + table.bonusRolls(context);
        if (!weighted.isEmpty()) {
            RandomSelector<LootEntry> selector = RandomSelector.weighted(weighted);
            for (int i = 0; i < totalRolls; i++) {
                addReward(rewards, errors, selector.pick(random), context);
            }
        }
        for (LootPool pool : table.pools()) {
            rewards.addAll(pool.generate(context, random, errors));
        }

        return new LootGenerationResult(table.id(), context, rewards, errors);
    }

    private static void addReward(List<LootReward> rewards, List<String> errors, LootEntry entry, LootContext context) {
        try {
            rewards.add(entry.create(context));
        } catch (RuntimeException e) {
            errors.add(entry.id() + ": " + e.getMessage());
        }
    }
}
