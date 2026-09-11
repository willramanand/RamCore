package dev.willram.ramcore.loot;

import dev.willram.ramcore.random.Weighted;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/**
 * Weighted entry inside a loot pool.
 */
public final class LootPoolEntry implements Weighted {
    private final String id;
    private final double weight;
    private final Function<LootContext, LootReward> rewardFactory;
    private final List<LootCondition> conditions;
    private final List<LootFunction> functions;

    private LootPoolEntry(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.weight = builder.weight;
        this.rewardFactory = Objects.requireNonNull(builder.rewardFactory, "rewardFactory");
        this.conditions = List.copyOf(builder.conditions);
        this.functions = List.copyOf(builder.functions);
        if (this.id.isBlank()) {
            throw new IllegalArgumentException("loot pool entry id must not be blank");
        }
        if (this.weight <= 0.0d) {
            throw new IllegalArgumentException("loot pool entry weight must be positive");
        }
    }

    @NotNull
    public static Builder builder(@NotNull String id, @NotNull LootReward reward) {
        return builder(id, context -> reward);
    }

    @NotNull
    public static Builder builder(@NotNull String id, @NotNull Function<LootContext, LootReward> rewardFactory) {
        return new Builder(id, rewardFactory);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public List<LootCondition> conditions() {
        return this.conditions;
    }

    @NotNull
    public List<LootFunction> functions() {
        return this.functions;
    }

    public boolean eligible(@NotNull LootContext context, @NotNull Random random) {
        for (LootCondition condition : this.conditions) {
            if (!condition.test(context, random)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public LootReward create(@NotNull LootContext context, @NotNull Random random) {
        LootReward reward = this.rewardFactory.apply(context);
        for (LootFunction function : this.functions) {
            reward = function.apply(reward, context, random);
        }
        return reward;
    }

    @Override
    public double getWeight() {
        return this.weight;
    }

    public static final class Builder {
        private final String id;
        private final Function<LootContext, LootReward> rewardFactory;
        private double weight = 1.0d;
        private final List<LootCondition> conditions = new ArrayList<>();
        private final List<LootFunction> functions = new ArrayList<>();

        private Builder(String id, Function<LootContext, LootReward> rewardFactory) {
            this.id = Objects.requireNonNull(id, "id");
            this.rewardFactory = Objects.requireNonNull(rewardFactory, "rewardFactory");
        }

        @NotNull
        public Builder weight(double weight) {
            this.weight = weight;
            return this;
        }

        @NotNull
        public Builder when(@NotNull LootCondition condition) {
            this.conditions.add(Objects.requireNonNull(condition, "condition"));
            return this;
        }

        @NotNull
        public Builder apply(@NotNull LootFunction function) {
            this.functions.add(Objects.requireNonNull(function, "function"));
            return this;
        }

        @NotNull
        public LootPoolEntry build() {
            return new LootPoolEntry(this);
        }
    }
}
