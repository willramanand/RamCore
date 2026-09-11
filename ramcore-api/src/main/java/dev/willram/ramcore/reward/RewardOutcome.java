package dev.willram.ramcore.reward;

import org.jetbrains.annotations.NotNull;

/**
 * One reward action result.
 */
public record RewardOutcome(
        @NotNull String id,
        boolean success,
        boolean preview,
        @NotNull String message
) {

    @NotNull
    public static RewardOutcome success(@NotNull String id) {
        return new RewardOutcome(id, true, false, "");
    }

    @NotNull
    public static RewardOutcome preview(@NotNull String id) {
        return new RewardOutcome(id, true, true, "");
    }

    @NotNull
    public static RewardOutcome failed(@NotNull String id, @NotNull String message) {
        return new RewardOutcome(id, false, false, message);
    }
}
