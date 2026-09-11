package dev.willram.ramcore.reward;

import org.jetbrains.annotations.NotNull;

/**
 * Facade for the reward pipeline.
 *
 * <pre>{@code
 * RewardPlan plan = Rewards.plan()
 *         .guaranteed(RewardEntry.guaranteed("money", RewardActions.money(economy, 100)))
 *         .weighted(RewardEntry.weighted("rare", RewardActions.item(sword), 1))
 *         .build();
 * RewardReport report = Rewards.engine().execute(plan, RewardContext.of("quest").withSubject(player), random);
 * }</pre>
 */
public final class Rewards {

    private Rewards() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * A new reward engine (stateless; reuse freely).
     *
     * @return the engine
     */
    @NotNull
    public static RewardEngine engine() {
        return new RewardEngine();
    }

    /**
     * A new reward plan builder.
     *
     * @return the builder
     */
    @NotNull
    public static RewardPlan.Builder plan() {
        return RewardPlan.builder();
    }
}
