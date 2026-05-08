package dev.willram.ramcore.encounter;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Boss phase selected by remaining health percentage.
 */
public final class EncounterPhase {
    private final String id;
    private final double atOrBelowHealthPercent;
    private final List<EncounterAbility> abilities = new ArrayList<>();

    private EncounterPhase(@NotNull String id, double atOrBelowHealthPercent) {
        this.id = validate(id, "encounter phase id");
        RamPreconditions.checkArgument(
                atOrBelowHealthPercent > 0.0d && atOrBelowHealthPercent <= 1.0d,
                "phase health percent must be within (0, 1]",
                "Use 1.0 for the opening phase, 0.5 for a half-health phase, etc."
        );
        this.atOrBelowHealthPercent = atOrBelowHealthPercent;
    }

    @NotNull
    public static EncounterPhase atOrBelow(@NotNull String id, double healthPercent) {
        return new EncounterPhase(id, healthPercent);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    public double atOrBelowHealthPercent() {
        return this.atOrBelowHealthPercent;
    }

    @NotNull
    public EncounterPhase ability(@NotNull EncounterAbility ability) {
        this.abilities.add(requireNonNull(ability, "ability"));
        return this;
    }

    @NotNull
    public List<EncounterAbility> abilities() {
        return List.copyOf(this.abilities);
    }

    @NotNull
    private static String validate(@NotNull String value, @NotNull String subject) {
        requireNonNull(value, subject);
        String trimmed = value.trim().toLowerCase();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_.:-]+"),
                subject + " contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, or colons."
        );
        return trimmed;
    }
}
