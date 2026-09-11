package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.stat.StatSnapshot;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A required minimum value of a stat before an ability may be cast, checked against the caster's
 * {@link StatSnapshot}.
 *
 * <p>Stats are read-only computed values with no mutable pool (see {@code dev.willram.ramcore.stat}),
 * so a cost is a <em>gate</em>, not something deducted. A consumer that wants a spendable resource
 * (mana, energy) backs it with its own store and does the deduction in the {@link AbilityAction}.</p>
 *
 * @param statId the stat to check
 * @param amount the minimum value required (non-negative)
 */
public record StatCost(@NotNull ContentId statId, double amount) {

    public StatCost {
        requireNonNull(statId, "statId");
        RamPreconditions.checkArgument(Double.isFinite(amount) && amount >= 0.0D,
                "stat cost amount must be finite and non-negative (was " + amount + ")",
                "pass a finite, non-negative cost amount");
    }

    /**
     * Whether the snapshot meets this cost.
     *
     * @param snapshot the caster's snapshot
     * @return {@code true} if {@code snapshot.value(statId) >= amount}
     */
    public boolean affordable(@NotNull StatSnapshot snapshot) {
        return snapshot.value(statId) >= this.amount;
    }
}
