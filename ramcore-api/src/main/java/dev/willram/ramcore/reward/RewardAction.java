package dev.willram.ramcore.reward;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Executable reward action.
 */
@FunctionalInterface
public interface RewardAction {

    @NotNull
    RewardOutcome apply(@NotNull RewardContext context);

    @NotNull
    default List<String> validate(@NotNull RewardContext context) {
        return List.of();
    }
}
