package dev.willram.ramcore.reward;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.random.Weighted;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Reward plan entry.
 */
public record RewardEntry(
        @NotNull String id,
        @NotNull RewardAction action,
        boolean guaranteed,
        double weight,
        @NotNull Predicate<RewardContext> condition
) implements Weighted {

    @NotNull
    public static RewardEntry guaranteed(@NotNull String id, @NotNull RewardAction action) {
        return new RewardEntry(id, action, true, 0, context -> true);
    }

    @NotNull
    public static RewardEntry weighted(@NotNull String id, @NotNull RewardAction action, double weight) {
        return new RewardEntry(id, action, false, weight, context -> true);
    }

    @NotNull
    public RewardEntry when(@NotNull Predicate<RewardContext> condition) {
        return new RewardEntry(this.id, this.action, this.guaranteed, this.weight, condition);
    }

    public RewardEntry {
        requireNonNull(id, "id");
        requireNonNull(action, "action");
        requireNonNull(condition, "condition");
        RamPreconditions.checkArgument(!id.isBlank(), "reward id must not be blank", "Use a stable reward id such as 'money' or 'quest_item'.");
        if (!guaranteed) {
            RamPreconditions.checkArgument(weight > 0, "weighted reward weight must be > 0", "Use a positive weight.");
        }
    }

    @Override
    public double getWeight() {
        return this.weight;
    }
}
