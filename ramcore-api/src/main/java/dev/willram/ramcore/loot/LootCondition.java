package dev.willram.ramcore.loot;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

/**
 * Random-aware loot condition.
 */
@FunctionalInterface
public interface LootCondition {

    boolean test(@NotNull LootContext context, @NotNull Random random);

    @NotNull
    default LootCondition and(@NotNull LootCondition other) {
        Objects.requireNonNull(other, "other");
        return (context, random) -> test(context, random) && other.test(context, random);
    }

    @NotNull
    default LootCondition or(@NotNull LootCondition other) {
        Objects.requireNonNull(other, "other");
        return (context, random) -> test(context, random) || other.test(context, random);
    }

    @NotNull
    default LootCondition negate() {
        return (context, random) -> !test(context, random);
    }
}
