package dev.willram.ramcore.loot;

import dev.willram.ramcore.random.RandomSelector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Weighted loot pool with its own rolls, conditions, and functions.
 */
public final class LootPool {
    private final String id;
    private final List<LootPoolEntry> entries;
    private final int rolls;
    private final ToIntFunction<LootContext> bonusRolls;
    private final List<LootCondition> conditions;
    private final List<LootFunction> functions;

    private LootPool(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.entries = List.copyOf(builder.entries);
        this.rolls = builder.rolls;
        this.bonusRolls = Objects.requireNonNull(builder.bonusRolls, "bonusRolls");
        this.conditions = List.copyOf(builder.conditions);
        this.functions = List.copyOf(builder.functions);
        if (this.id.isBlank()) {
            throw new IllegalArgumentException("loot pool id must not be blank");
        }
        if (this.rolls < 0) {
            throw new IllegalArgumentException("loot pool rolls cannot be negative");
        }
    }

    @NotNull
    public static Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public List<LootPoolEntry> entries() {
        return this.entries;
    }

    public int rolls() {
        return this.rolls;
    }

    public int bonusRolls(@NotNull LootContext context) {
        return Math.max(0, this.bonusRolls.applyAsInt(context));
    }

    @NotNull
    public List<LootReward> generate(@NotNull LootContext context, @NotNull Random random, @NotNull List<String> errors) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(errors, "errors");
        for (LootCondition condition : this.conditions) {
            if (!condition.test(context, random)) {
                return List.of();
            }
        }
        List<LootPoolEntry> eligible = this.entries.stream()
                .filter(entry -> entry.eligible(context, random))
                .toList();
        if (eligible.isEmpty()) {
            return List.of();
        }
        RandomSelector<LootPoolEntry> selector = RandomSelector.weighted(eligible);
        List<LootReward> rewards = new ArrayList<>();
        int totalRolls = this.rolls + bonusRolls(context);
        for (int i = 0; i < totalRolls; i++) {
            LootPoolEntry entry = selector.pick(random);
            try {
                LootReward reward = entry.create(context, random);
                for (LootFunction function : this.functions) {
                    reward = function.apply(reward, context, random);
                }
                rewards.add(reward);
            } catch (RuntimeException e) {
                errors.add(this.id + "/" + entry.id() + ": " + e.getMessage());
            }
        }
        return rewards;
    }

    public static final class Builder {
        private final String id;
        private final List<LootPoolEntry> entries = new ArrayList<>();
        private int rolls = 1;
        private ToIntFunction<LootContext> bonusRolls = context -> 0;
        private final List<LootCondition> conditions = new ArrayList<>();
        private final List<LootFunction> functions = new ArrayList<>();

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        @NotNull
        public Builder entry(@NotNull LootPoolEntry entry) {
            this.entries.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        @NotNull
        public Builder rolls(int rolls) {
            this.rolls = rolls;
            return this;
        }

        @NotNull
        public Builder bonusRolls(@NotNull ToIntFunction<LootContext> bonusRolls) {
            this.bonusRolls = Objects.requireNonNull(bonusRolls, "bonusRolls");
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
        public LootPool build() {
            return new LootPool(this);
        }
    }
}
