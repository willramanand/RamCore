package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Pending invitation for a player to join a party.
 */
public record PartyInvite(
        @NotNull PartyId partyId,
        @NotNull UUID target,
        @NotNull UUID invitedBy,
        @NotNull Instant createdAt,
        @NotNull Instant expiresAt
) {

    public PartyInvite {
        requireNonNull(partyId, "partyId");
        requireNonNull(target, "target");
        requireNonNull(invitedBy, "invitedBy");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiresAt, "expiresAt");
    }

    public boolean expired(@NotNull Clock clock) {
        return !expiresAt.isAfter(Instant.now(requireNonNull(clock, "clock")));
    }
}
