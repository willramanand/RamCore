package dev.willram.ramcore.party;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * Configuration for in-memory party managers.
 */
public record PartyOptions(
        int maxMembers,
        @NotNull Duration inviteTtl,
        boolean disbandWhenLeaderLeaves
) {

    public PartyOptions {
        requireNonNull(inviteTtl, "inviteTtl");
        RamPreconditions.checkArgument(maxMembers > 0, "max members must be positive", "Use at least 1.");
        RamPreconditions.checkArgument(!inviteTtl.isNegative() && !inviteTtl.isZero(), "invite ttl must be positive", "Use a positive duration.");
    }

    @NotNull
    public static PartyOptions defaults() {
        return new PartyOptions(5, Duration.ofSeconds(60), true);
    }

    @NotNull
    public PartyOptions maxMembers(int maxMembers) {
        return new PartyOptions(maxMembers, this.inviteTtl, this.disbandWhenLeaderLeaves);
    }

    @NotNull
    public PartyOptions inviteTtl(@NotNull Duration inviteTtl) {
        return new PartyOptions(this.maxMembers, inviteTtl, this.disbandWhenLeaderLeaves);
    }

    @NotNull
    public PartyOptions disbandWhenLeaderLeaves(boolean disbandWhenLeaderLeaves) {
        return new PartyOptions(this.maxMembers, this.inviteTtl, disbandWhenLeaderLeaves);
    }
}
