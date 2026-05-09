package dev.willram.ramcore.loot;

import dev.willram.ramcore.random.VariableAmount;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Common loot reward transformations.
 */
public final class LootFunctions {

    @NotNull
    public static LootFunction amount(@NotNull VariableAmount amount) {
        Objects.requireNonNull(amount, "amount");
        return (reward, context, random) -> new LootReward(
                reward.id(),
                reward.payload(),
                Math.max(1, amount.getFlooredAmount(random)),
                reward.metadata()
        );
    }

    @NotNull
    public static LootFunction multiplyAmount(double multiplier) {
        if (multiplier <= 0.0d) {
            throw new IllegalArgumentException("multiplier must be positive");
        }
        return (reward, context, random) -> new LootReward(
                reward.id(),
                reward.payload(),
                Math.max(1, (int) Math.floor(reward.amount() * multiplier)),
                reward.metadata()
        );
    }

    @NotNull
    public static LootFunction metadata(@NotNull String key, @NotNull Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return (reward, context, random) -> {
            Map<String, Object> metadata = new LinkedHashMap<>(reward.metadata());
            metadata.put(key, value);
            return reward.withMetadata(metadata);
        };
    }

    @NotNull
    public static LootFunction payload(@NotNull BiFunction<LootReward, LootContext, Object> transformer) {
        Objects.requireNonNull(transformer, "transformer");
        return (reward, context, random) -> new LootReward(
                reward.id(),
                transformer.apply(reward, context),
                reward.amount(),
                reward.metadata()
        );
    }

    private LootFunctions() {
    }
}
