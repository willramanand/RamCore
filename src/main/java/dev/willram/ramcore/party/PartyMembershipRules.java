package dev.willram.ramcore.party;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

/**
 * Common party membership rules.
 */
public final class PartyMembershipRules {

    @NotNull
    public static PartyMembershipRule maxMembers(int maxMembers) {
        RamPreconditions.checkArgument(maxMembers > 0, "max members must be positive", "Use at least 1.");
        return (party, ignored) -> party.size() < maxMembers
                ? PartyResult.ok()
                : PartyResult.failure("party is full");
    }

    private PartyMembershipRules() {
    }
}
