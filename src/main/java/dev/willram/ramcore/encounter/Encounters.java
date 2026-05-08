package dev.willram.ramcore.encounter;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Entry points for RamCore encounter helpers.
 */
public final class Encounters {

    @NotNull
    public static EncounterRegistry registry() {
        return new EncounterRegistry(List.of());
    }

    @NotNull
    public static EncounterRegistry registry(@NotNull EncounterListener listener) {
        return new EncounterRegistry(List.of(listener));
    }

    @NotNull
    public static EncounterDefinition.Builder encounter(@NotNull ContentId id, double maxHealth) {
        return EncounterDefinition.builder(id, maxHealth);
    }

    @NotNull
    public static EncounterPhase phase(@NotNull String id, double atOrBelowHealthPercent) {
        return EncounterPhase.atOrBelow(id, atOrBelowHealthPercent);
    }

    @NotNull
    public static EncounterAbility ability(@NotNull String id, long intervalTicks) {
        return EncounterAbility.every(id, intervalTicks);
    }

    private Encounters() {
    }
}
