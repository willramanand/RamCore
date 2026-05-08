package dev.willram.ramcore.region;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Region rule evaluation result with source metadata.
 */
public record RegionDecision(
        @NotNull RegionRuleResult result,
        @Nullable ContentId region,
        @Nullable String rule
) {

    @NotNull
    public static RegionDecision pass() {
        return new RegionDecision(RegionRuleResult.PASS, null, null);
    }

    @NotNull
    public static RegionDecision of(@NotNull RegionRuleResult result, @NotNull ContentId region, @NotNull String rule) {
        return new RegionDecision(result, region, rule);
    }

    public boolean allowed() {
        return this.result == RegionRuleResult.ALLOW;
    }

    public boolean denied() {
        return this.result == RegionRuleResult.DENY;
    }
}
