package dev.willram.ramcore.region;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Region shape plus priority-ordered rules.
 */
public final class RuleRegion {
    private final ContentId id;
    private final RegionShape shape;
    private final int priority;
    private final List<RegionRule> rules;

    private RuleRegion(@NotNull ContentId id, @NotNull RegionShape shape, int priority, @NotNull Collection<RegionRule> rules) {
        this.id = id;
        this.shape = shape;
        this.priority = priority;
        this.rules = List.copyOf(rules);
    }

    @NotNull
    public static Builder builder(@NotNull ContentId id, @NotNull RegionShape shape) {
        return new Builder(id, shape);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    @NotNull
    public RegionShape shape() {
        return this.shape;
    }

    public int priority() {
        return this.priority;
    }

    @NotNull
    public List<RegionRule> rules() {
        return this.rules;
    }

    public boolean contains(@NotNull RegionQuery query) {
        return this.shape.contains(query.position());
    }

    @NotNull
    public RegionDecision evaluate(@NotNull RegionQuery query) {
        if (!contains(query)) {
            return RegionDecision.pass();
        }

        return this.rules.stream()
                .filter(rule -> rule.matches(query))
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .filter(rule -> rule.result() != RegionRuleResult.PASS)
                .findFirst()
                .map(rule -> RegionDecision.of(rule.result(), this.id, rule.id()))
                .orElseGet(RegionDecision::pass);
    }

    public static final class Builder {
        private final ContentId id;
        private final RegionShape shape;
        private final List<RegionRule> rules = new ArrayList<>();
        private int priority;

        private Builder(ContentId id, RegionShape shape) {
            this.id = id;
            this.shape = shape;
        }

        @NotNull
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @NotNull
        public Builder rule(@NotNull RegionRule rule) {
            this.rules.add(rule);
            return this;
        }

        @NotNull
        public RuleRegion build() {
            return new RuleRegion(this.id, this.shape, this.priority, this.rules);
        }
    }
}
