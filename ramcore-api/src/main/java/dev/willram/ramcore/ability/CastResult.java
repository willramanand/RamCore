package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * The result of an {@link AbilityCaster#cast} attempt: the status, which ability, the cooldown
 * remaining (for {@link AbilityCastStatus#ON_COOLDOWN}), and any validation errors (for
 * {@link AbilityCastStatus#INVALID}).
 *
 * @param status          the outcome
 * @param abilityId       the ability
 * @param remainingMillis cooldown remaining, or {@code 0}
 * @param errors          validation errors, or empty
 */
public record CastResult(@NotNull AbilityCastStatus status, @NotNull ContentId abilityId,
                         long remainingMillis, @NotNull List<String> errors) {

    public CastResult {
        requireNonNull(status, "status");
        requireNonNull(abilityId, "abilityId");
        errors = List.copyOf(errors);
    }

    @NotNull
    static CastResult of(@NotNull AbilityCastStatus status, @NotNull ContentId abilityId) {
        return new CastResult(status, abilityId, 0L, List.of());
    }

    @NotNull
    static CastResult onCooldown(@NotNull ContentId abilityId, long remainingMillis) {
        return new CastResult(AbilityCastStatus.ON_COOLDOWN, abilityId, remainingMillis, List.of());
    }

    @NotNull
    static CastResult invalid(@NotNull ContentId abilityId, @NotNull List<String> errors) {
        return new CastResult(AbilityCastStatus.INVALID, abilityId, 0L, errors);
    }

    /** Whether the cast started (instant executed or channel began). */
    public boolean started() {
        return this.status.started();
    }
}
