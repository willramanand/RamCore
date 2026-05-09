package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Result of rolling a loot table.
 */
public record LootGenerationResult(
        @NotNull ContentId tableId,
        @NotNull LootContext context,
        @NotNull List<LootReward> rewards,
        @NotNull List<String> errors
) {
    public LootGenerationResult {
        rewards = List.copyOf(rewards);
        errors = List.copyOf(errors);
    }

    public boolean successful() {
        return this.errors.isEmpty();
    }
}
