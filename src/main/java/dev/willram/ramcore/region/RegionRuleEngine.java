package dev.willram.ramcore.region;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import dev.willram.ramcore.content.ContentRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Registry-backed lightweight region rule evaluator.
 */
public final class RegionRuleEngine implements AutoCloseable {
    private final ContentRegistry<RuleRegion> regions = ContentRegistry.create(RuleRegion.class);

    @NotNull
    public RuleRegion register(@NotNull String owner, @NotNull RuleRegion region) {
        ContentKey<RuleRegion> key = ContentKey.of(region.id(), RuleRegion.class);
        return this.regions.register(owner, key, region).value();
    }

    @NotNull
    public RegionDecision evaluate(@NotNull RegionQuery query) {
        return this.regions.entries().stream()
                .map(entry -> entry.value())
                .filter(region -> region.contains(query))
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .map(region -> region.evaluate(query))
                .filter(decision -> decision.result() != RegionRuleResult.PASS)
                .findFirst()
                .orElseGet(RegionDecision::pass);
    }

    public int unregisterOwner(@NotNull String owner) {
        return this.regions.unregisterOwner(owner);
    }

    public boolean contains(@NotNull ContentId id) {
        return this.regions.contains(id);
    }

    @Override
    public void close() {
        this.regions.close();
    }
}
