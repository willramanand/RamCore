package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Validates whether a player may join a party.
 */
@FunctionalInterface
public interface PartyMembershipRule {

    @NotNull
    PartyResult<Void> test(@NotNull PartyGroup party, @NotNull UUID playerId);
}
