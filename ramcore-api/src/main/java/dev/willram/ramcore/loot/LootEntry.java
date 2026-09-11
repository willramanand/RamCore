package dev.willram.ramcore.loot;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.random.Weighted;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One loot table entry.
 */
public record LootEntry(
        @NotNull String id,
        double weight,
        @NotNull Function<LootContext, LootReward> rewardFactory,
        @NotNull Predicate<LootContext> condition
) implements Weighted {

    @NotNull
    public static LootEntry guaranteed(@NotNull String id, @NotNull LootReward reward) {
        return new LootEntry(id, 1.0d, context -> reward, context -> true);
    }

    @NotNull
    public static LootEntry guaranteed(@NotNull String id, @NotNull Function<LootContext, LootReward> rewardFactory) {
        return new LootEntry(id, 1.0d, rewardFactory, context -> true);
    }

    @NotNull
    public static LootEntry weighted(@NotNull String id, double weight, @NotNull LootReward reward) {
        return new LootEntry(id, weight, context -> reward, context -> true);
    }

    @NotNull
    public static LootEntry weighted(@NotNull String id, double weight, @NotNull Function<LootContext, LootReward> rewardFactory) {
        return new LootEntry(id, weight, rewardFactory, context -> true);
    }

    public LootEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rewardFactory, "rewardFactory");
        Objects.requireNonNull(condition, "condition");
        RamPreconditions.checkArgument(!id.isBlank(), "loot entry id must not be blank", "Use a stable id such as 'rare_gem'.");
        RamPreconditions.checkArgument(weight > 0.0d, "loot entry weight must be > 0", "Use a positive weight.");
    }

    @NotNull
    public LootEntry when(@NotNull Predicate<LootContext> condition) {
        return new LootEntry(this.id, this.weight, this.rewardFactory, condition);
    }

    @NotNull
    public LootReward create(@NotNull LootContext context) {
        return this.rewardFactory.apply(Objects.requireNonNull(context, "context"));
    }

    @Override
    public double getWeight() {
        return this.weight;
    }
}
