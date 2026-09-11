package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The persisted shape of a party: id, leader, and member roles. Invites, metadata and
 * contribution tracking are transient and are not stored.
 *
 * @param id     the party id string
 * @param leader the leader
 * @param roles  every member (including the leader) with their role
 */
public record PartySnapshot(@NotNull String id, @NotNull UUID leader, @NotNull Map<UUID, PartyRole> roles) {

    public PartySnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(leader, "leader");
        roles = Map.copyOf(Objects.requireNonNull(roles, "roles"));
    }

    /**
     * Captures a party's persistent state.
     *
     * @param party the party
     * @return the snapshot
     */
    @NotNull
    public static PartySnapshot of(@NotNull PartyGroup party) {
        Objects.requireNonNull(party, "party");
        return new PartySnapshot(party.id().toString(), party.leader(), party.roles());
    }

    @NotNull
    public PartyId partyId() {
        return PartyId.of(this.id);
    }
}
