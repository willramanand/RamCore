package dev.willram.ramcore.party;

import dev.willram.ramcore.metadata.MetadataMap;
import dev.willram.ramcore.reward.RewardContext;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * In-memory party state.
 */
public final class PartyGroup {
    private final PartyId id;
    private final Map<UUID, PartyRole> members = new LinkedHashMap<>();
    private final Map<UUID, PartyInvite> invites = new LinkedHashMap<>();
    private final MetadataMap metadata = MetadataMap.create();
    private final PartyContributionTracker contributions = new PartyContributionTracker();
    private UUID leader;

    PartyGroup(@NotNull PartyId id, @NotNull UUID leader) {
        this.id = requireNonNull(id, "id");
        this.leader = requireNonNull(leader, "leader");
        this.members.put(leader, PartyRole.LEADER);
    }

    @NotNull
    public PartyId id() {
        return this.id;
    }

    @NotNull
    public UUID leader() {
        return this.leader;
    }

    public int size() {
        return this.members.size();
    }

    public boolean contains(@NotNull UUID playerId) {
        return this.members.containsKey(requireNonNull(playerId, "playerId"));
    }

    public boolean leader(@NotNull UUID playerId) {
        return this.leader.equals(requireNonNull(playerId, "playerId"));
    }

    @NotNull
    public Set<UUID> members() {
        return Set.copyOf(this.members.keySet());
    }

    @NotNull
    public Map<UUID, PartyRole> roles() {
        return Map.copyOf(this.members);
    }

    @NotNull
    public Map<UUID, PartyInvite> invites() {
        return Map.copyOf(this.invites);
    }

    @NotNull
    public MetadataMap metadata() {
        return this.metadata;
    }

    @NotNull
    public PartyContributionTracker contributions() {
        return this.contributions;
    }

    @NotNull
    public RewardContext rewardContext(@NotNull String scope) {
        return RewardContext.of(scope)
                .withSubject(this)
                .withMetadata(Map.of(
                        "partyId", this.id.toString(),
                        "leader", this.leader,
                        "members", members()
                ));
    }

    void invite(@NotNull PartyInvite invite) {
        this.invites.put(invite.target(), invite);
    }

    boolean hasInvite(@NotNull UUID playerId) {
        return this.invites.containsKey(playerId);
    }

    PartyInvite invite(@NotNull UUID playerId) {
        return this.invites.get(playerId);
    }

    void removeInvite(@NotNull UUID playerId) {
        this.invites.remove(playerId);
    }

    void addMember(@NotNull UUID playerId) {
        this.members.put(playerId, PartyRole.MEMBER);
    }

    void removeMember(@NotNull UUID playerId) {
        this.members.remove(playerId);
        this.invites.remove(playerId);
    }

    void promote(@NotNull UUID playerId) {
        requireNonNull(playerId, "playerId");
        this.members.put(this.leader, PartyRole.MEMBER);
        this.leader = playerId;
        this.members.put(playerId, PartyRole.LEADER);
    }

    void cleanup() {
        this.members.clear();
        this.invites.clear();
        this.metadata.clear();
        this.contributions.clear();
    }
}
