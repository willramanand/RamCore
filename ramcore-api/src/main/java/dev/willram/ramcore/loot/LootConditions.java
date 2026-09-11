package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Common loot condition factories.
 */
public final class LootConditions {
    private static final LootCondition ALWAYS = (context, random) -> true;

    @NotNull
    public static LootCondition always() {
        return ALWAYS;
    }

    @NotNull
    public static LootCondition chance(double chance) {
        if (chance < 0.0d || chance > 1.0d) {
            throw new IllegalArgumentException("chance must be between 0 and 1");
        }
        return (context, random) -> random.nextDouble() < chance;
    }

    @NotNull
    public static LootCondition luckAtLeast(double luck) {
        return (context, random) -> context.luck() >= luck;
    }

    @NotNull
    public static LootCondition world(@NotNull String worldName) {
        Objects.requireNonNull(worldName, "worldName");
        return (context, random) -> worldName.equals(context.worldName());
    }

    @NotNull
    public static LootCondition metadataEquals(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key");
        return (context, random) -> Objects.equals(context.metadata().get(key), value);
    }

    @NotNull
    public static LootCondition all(@NotNull LootCondition... conditions) {
        LootCondition[] copy = copy(conditions);
        return (context, random) -> Arrays.stream(copy).allMatch(condition -> condition.test(context, random));
    }

    @NotNull
    public static LootCondition any(@NotNull LootCondition... conditions) {
        LootCondition[] copy = copy(conditions);
        return (context, random) -> Arrays.stream(copy).anyMatch(condition -> condition.test(context, random));
    }

    private static LootCondition[] copy(LootCondition[] conditions) {
        Objects.requireNonNull(conditions, "conditions");
        LootCondition[] copy = conditions.clone();
        for (LootCondition condition : copy) {
            Objects.requireNonNull(condition, "condition");
        }
        return copy;
    }

    private LootConditions() {
    }
}
