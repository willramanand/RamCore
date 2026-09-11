package dev.willram.ramcore.region;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Priority rule for one action category.
 */
public record RegionRule(
        @NotNull String id,
        @NotNull RegionAction action,
        int priority,
        @NotNull RegionRuleResult result,
        @NotNull Predicate<RegionQuery> condition
) {

    @NotNull
    public static RegionRule of(@NotNull String id, @NotNull RegionAction action, int priority, @NotNull RegionRuleResult result) {
        return new RegionRule(id, action, priority, result, query -> true);
    }

    @NotNull
    public RegionRule when(@NotNull Predicate<RegionQuery> condition) {
        return new RegionRule(this.id, this.action, this.priority, this.result, condition);
    }

    public RegionRule {
        requireNonNull(id, "id");
        requireNonNull(action, "action");
        requireNonNull(result, "result");
        requireNonNull(condition, "condition");
        if (id.isBlank()) {
            throw new IllegalArgumentException("rule id must not be blank");
        }
    }

    boolean matches(RegionQuery query) {
        return this.action == query.action() && this.condition.test(query);
    }
}
