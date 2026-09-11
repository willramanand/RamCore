package dev.willram.ramcore.objective;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.party.PartyId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Scope key for objective progress.
 */
public record ObjectiveSubject(@NotNull String type, @NotNull String id) {

    public ObjectiveSubject {
        type = validate(type, "objective subject type");
        id = validate(id, "objective subject id");
    }

    @NotNull
    public static ObjectiveSubject player(@NotNull UUID playerId) {
        return new ObjectiveSubject("player", requireNonNull(playerId, "playerId").toString());
    }

    @NotNull
    public static ObjectiveSubject party(@NotNull PartyId partyId) {
        return new ObjectiveSubject("party", requireNonNull(partyId, "partyId").toString());
    }

    @NotNull
    public static ObjectiveSubject global(@NotNull String id) {
        return new ObjectiveSubject("global", id);
    }

    @NotNull
    public static ObjectiveSubject of(@NotNull String type, @NotNull String id) {
        return new ObjectiveSubject(type, id);
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
