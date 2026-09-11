package dev.willram.ramcore.ability;

import dev.willram.ramcore.stat.StatSnapshot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Everything an {@link AbilityAction} or effect needs for one cast: who cast it, the resolved
 * targets, the caster's stat snapshot at cast time, and what triggered it. {@code metadata} carries
 * arbitrary extra state (e.g. a channel tick, a combo step).
 *
 * @param caster   the casting player
 * @param targets  the resolved targets (may be empty)
 * @param snapshot the caster's stats at cast time
 * @param trigger  what triggered the cast
 * @param metadata free-form context data
 */
public record AbilityContext(@NotNull Player caster, @NotNull List<LivingEntity> targets,
                             @NotNull StatSnapshot snapshot, @NotNull AbilityTrigger trigger,
                             @NotNull Map<String, Object> metadata) {

    public AbilityContext {
        requireNonNull(caster, "caster");
        requireNonNull(snapshot, "snapshot");
        requireNonNull(trigger, "trigger");
        targets = List.copyOf(targets);
        metadata = Map.copyOf(metadata);
    }

    /**
     * A context with no extra metadata.
     *
     * @param caster   the caster
     * @param targets  the targets
     * @param snapshot the snapshot
     * @param trigger  the trigger
     * @return the context
     */
    @NotNull
    public static AbilityContext of(@NotNull Player caster, @NotNull List<LivingEntity> targets,
                                    @NotNull StatSnapshot snapshot, @NotNull AbilityTrigger trigger) {
        return new AbilityContext(caster, targets, snapshot, trigger, Map.of());
    }
}
