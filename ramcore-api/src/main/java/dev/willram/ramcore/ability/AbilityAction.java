package dev.willram.ramcore.ability;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The logic an ability runs on a successful cast. Mirrors {@code reward.RewardAction}: an optional
 * {@link #validate(AbilityContext)} runs before the cast is committed (aggregated into the cast's
 * validation errors), then {@link #run(AbilityContext)} applies the effect.
 */
@FunctionalInterface
public interface AbilityAction {

    /** An action that does nothing. */
    AbilityAction NONE = context -> {
    };

    /**
     * Applies the ability. Called on the caster's thread after cooldown, cost, and validation pass.
     *
     * @param context the cast context
     */
    void run(@NotNull AbilityContext context);

    /**
     * Reasons this action cannot run for the given context, or an empty list when it can. Checked
     * before the cast is committed.
     *
     * @param context the cast context
     * @return the validation errors
     */
    @NotNull
    default List<String> validate(@NotNull AbilityContext context) {
        return List.of();
    }
}
