package dev.willram.ramcore.encounter;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.party.PartyContributionTracker;
import dev.willram.ramcore.reward.RewardContext;
import dev.willram.ramcore.serialize.Position;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Mutable runtime state for one encounter run.
 */
public final class EncounterInstance {
    private final EncounterDefinition definition;
    private final List<EncounterListener> listeners = new ArrayList<>();
    private final Map<String, Long> nextAbilityTicks = new LinkedHashMap<>();
    private final PartyContributionTracker contributions = new PartyContributionTracker();
    private EncounterState state = EncounterState.READY;
    private EncounterPhase phase;
    private double health;
    private long elapsedTicks;

    EncounterInstance(@NotNull EncounterDefinition definition) {
        this.definition = requireNonNull(definition, "definition");
        this.health = definition.maxHealth();
        this.phase = definition.phaseFor(this.health);
        resetAbilityTimers();
    }

    @NotNull
    public EncounterDefinition definition() {
        return this.definition;
    }

    @NotNull
    public EncounterState state() {
        return this.state;
    }

    @NotNull
    public EncounterPhase phase() {
        return this.phase;
    }

    public double health() {
        return this.health;
    }

    public double healthPercent() {
        return this.health / this.definition.maxHealth();
    }

    public long elapsedTicks() {
        return this.elapsedTicks;
    }

    @NotNull
    public PartyContributionTracker contributions() {
        return this.contributions;
    }

    @NotNull
    public EncounterInstance listener(@NotNull EncounterListener listener) {
        this.listeners.add(requireNonNull(listener, "listener"));
        return this;
    }

    public void start() {
        if (this.state == EncounterState.RUNNING || this.state == EncounterState.ENRAGED) {
            return;
        }
        this.state = EncounterState.RUNNING;
        emit(EncounterSignal.START, this.phase.id());
    }

    public void tick() {
        if (this.state != EncounterState.RUNNING && this.state != EncounterState.ENRAGED) {
            return;
        }
        this.elapsedTicks++;
        if (this.definition.enrageAfterTicks() > 0 && this.elapsedTicks >= this.definition.enrageAfterTicks() && this.state == EncounterState.RUNNING) {
            this.state = EncounterState.ENRAGED;
            emit(EncounterSignal.ENRAGE, this.phase.id());
        }
        runAbilities();
        emit(EncounterSignal.TICK, Long.toString(this.elapsedTicks));
    }

    public void damage(@NotNull UUID source, double amount) {
        requireNonNull(source, "source");
        RamPreconditions.checkArgument(amount >= 0.0d, "encounter damage must be non-negative", "Use zero or a positive amount.");
        if (this.state != EncounterState.RUNNING && this.state != EncounterState.ENRAGED) {
            return;
        }
        this.contributions.add(source, amount);
        this.health = Math.max(0.0d, this.health - amount);
        emit(EncounterSignal.DAMAGE, Double.toString(amount));
        EncounterPhase nextPhase = this.definition.phaseFor(this.health);
        if (!nextPhase.id().equals(this.phase.id())) {
            this.phase = nextPhase;
            resetAbilityTimers();
            emit(EncounterSignal.PHASE_CHANGE, this.phase.id());
        }
        if (this.health <= 0.0d) {
            complete();
        }
    }

    public boolean withinArena(@NotNull Position position) {
        requireNonNull(position, "position");
        return this.definition.arena().isEmpty() || this.definition.arena().orElseThrow().contains(position);
    }

    public void wipe(@NotNull String reason) {
        if (this.state == EncounterState.COMPLETED) {
            return;
        }
        this.state = EncounterState.WIPED;
        emit(EncounterSignal.WIPE, reason);
    }

    public void reset(@NotNull String reason) {
        this.state = EncounterState.RESET;
        this.health = this.definition.maxHealth();
        this.elapsedTicks = 0L;
        this.phase = this.definition.phaseFor(this.health);
        this.contributions.clear();
        resetAbilityTimers();
        emit(EncounterSignal.RESET, reason);
    }

    public void complete() {
        if (this.state == EncounterState.COMPLETED) {
            return;
        }
        this.state = EncounterState.COMPLETED;
        this.health = 0.0d;
        emit(EncounterSignal.COMPLETE, this.phase.id());
    }

    @NotNull
    public RewardContext rewardContext(@NotNull String scope) {
        return RewardContext.of(scope)
                .withSubject(this)
                .withMetadata(Map.of(
                        "encounterId", this.definition.id().toString(),
                        "phase", this.phase.id(),
                        "state", this.state.name(),
                        "elapsedTicks", this.elapsedTicks,
                        "contributors", this.contributions.snapshot()
                ));
    }

    private void runAbilities() {
        for (EncounterAbility ability : this.phase.abilities()) {
            long nextTick = this.nextAbilityTicks.getOrDefault(ability.id(), ability.initialDelayTicks());
            if (this.elapsedTicks < nextTick) {
                continue;
            }
            ability.execute(this);
            emit(EncounterSignal.ABILITY, ability.id());
            this.nextAbilityTicks.put(ability.id(), this.elapsedTicks + ability.intervalTicks());
        }
    }

    private void resetAbilityTimers() {
        this.nextAbilityTicks.clear();
        for (EncounterAbility ability : this.phase.abilities()) {
            this.nextAbilityTicks.put(ability.id(), this.elapsedTicks + ability.initialDelayTicks());
        }
    }

    private void emit(@NotNull EncounterSignal signal, @NotNull String detail) {
        EncounterUpdate update = new EncounterUpdate(this, signal, detail);
        this.listeners.forEach(listener -> listener.update(update));
    }
}
