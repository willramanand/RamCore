package dev.willram.ramcore.reward;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reward validation or execution report.
 */
public record RewardReport(
        boolean preview,
        @NotNull List<RewardOutcome> outcomes,
        @NotNull List<String> errors
) {

    public boolean successful() {
        return this.errors.isEmpty() && this.outcomes.stream().allMatch(RewardOutcome::success);
    }
}
