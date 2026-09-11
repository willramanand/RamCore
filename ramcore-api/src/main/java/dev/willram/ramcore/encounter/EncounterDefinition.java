package dev.willram.ramcore.encounter;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.region.RegionShape;
import dev.willram.ramcore.reward.RewardPlan;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Reusable boss encounter definition.
 */
public final class EncounterDefinition {
    private final ContentId id;
    private final double maxHealth;
    private final List<EncounterPhase> phases;
    private final long enrageAfterTicks;
    private final RegionShape arena;
    private final RewardPlan rewards;

    private EncounterDefinition(@NotNull Builder builder) {
        this.id = builder.id;
        this.maxHealth = builder.maxHealth;
        this.phases = builder.phases.stream()
                .sorted(Comparator.comparingDouble(EncounterPhase::atOrBelowHealthPercent))
                .toList();
        this.enrageAfterTicks = builder.enrageAfterTicks;
        this.arena = builder.arena;
        this.rewards = builder.rewards;
        RamPreconditions.checkArgument(this.maxHealth > 0.0d, "encounter max health must be positive", "Use a positive health value.");
        RamPreconditions.checkArgument(!this.phases.isEmpty(), "encounter must contain at least one phase", "Add an opening phase at health percent 1.0.");
    }

    @NotNull
    public static Builder builder(@NotNull ContentId id, double maxHealth) {
        return new Builder(id, maxHealth);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    public double maxHealth() {
        return this.maxHealth;
    }

    @NotNull
    public List<EncounterPhase> phases() {
        return this.phases;
    }

    public long enrageAfterTicks() {
        return this.enrageAfterTicks;
    }

    @NotNull
    public Optional<RegionShape> arena() {
        return Optional.ofNullable(this.arena);
    }

    @NotNull
    public Optional<RewardPlan> rewards() {
        return Optional.ofNullable(this.rewards);
    }

    @NotNull
    public EncounterPhase phaseFor(double currentHealth) {
        double percent = Math.max(0.0d, currentHealth) / this.maxHealth;
        return this.phases.stream()
                .filter(phase -> percent <= phase.atOrBelowHealthPercent())
                .findFirst()
                .orElse(this.phases.get(this.phases.size() - 1));
    }

    public static final class Builder {
        private final ContentId id;
        private final double maxHealth;
        private final List<EncounterPhase> phases = new ArrayList<>();
        private long enrageAfterTicks = -1L;
        private RegionShape arena;
        private RewardPlan rewards;

        private Builder(@NotNull ContentId id, double maxHealth) {
            this.id = requireNonNull(id, "id");
            this.maxHealth = maxHealth;
        }

        @NotNull
        public Builder phase(@NotNull EncounterPhase phase) {
            this.phases.add(requireNonNull(phase, "phase"));
            return this;
        }

        @NotNull
        public Builder enrageAfterTicks(long enrageAfterTicks) {
            RamPreconditions.checkArgument(enrageAfterTicks > 0, "enrage ticks must be positive", "Use a positive tick count.");
            this.enrageAfterTicks = enrageAfterTicks;
            return this;
        }

        @NotNull
        public Builder arena(@NotNull RegionShape arena) {
            this.arena = requireNonNull(arena, "arena");
            return this;
        }

        @NotNull
        public Builder rewards(@NotNull RewardPlan rewards) {
            this.rewards = requireNonNull(rewards, "rewards");
            return this;
        }

        @NotNull
        public EncounterDefinition build() {
            return new EncounterDefinition(this);
        }
    }
}
