package dev.willram.ramcore.reward;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward pipeline with guaranteed entries and weighted rolls.
 */
public final class RewardPlan {
    private final List<RewardEntry> guaranteed;
    private final List<RewardEntry> weighted;
    private final int rolls;

    private RewardPlan(List<RewardEntry> guaranteed, List<RewardEntry> weighted, int rolls) {
        this.guaranteed = List.copyOf(guaranteed);
        this.weighted = List.copyOf(weighted);
        this.rolls = rolls;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public List<RewardEntry> guaranteed() {
        return this.guaranteed;
    }

    @NotNull
    public List<RewardEntry> weighted() {
        return this.weighted;
    }

    public int rolls() {
        return this.rolls;
    }

    public static final class Builder {
        private final List<RewardEntry> guaranteed = new ArrayList<>();
        private final List<RewardEntry> weighted = new ArrayList<>();
        private int rolls = 1;

        @NotNull
        public Builder guaranteed(@NotNull RewardEntry entry) {
            this.guaranteed.add(entry);
            return this;
        }

        @NotNull
        public Builder weighted(@NotNull RewardEntry entry) {
            this.weighted.add(entry);
            return this;
        }

        @NotNull
        public Builder rolls(int rolls) {
            this.rolls = rolls;
            return this;
        }

        @NotNull
        public RewardPlan build() {
            if (this.rolls < 0) {
                throw new IllegalArgumentException("reward rolls must be >= 0");
            }
            return new RewardPlan(this.guaranteed, this.weighted, this.rolls);
        }
    }
}
