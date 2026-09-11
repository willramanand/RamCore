package dev.willram.ramcore.party;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * In-memory party manager with one-party-per-player indexing.
 */
public final class PartyManager {
    private final Map<PartyId, PartyGroup> parties = new LinkedHashMap<>();
    private final Map<UUID, PartyId> memberIndex = new LinkedHashMap<>();
    private final List<PartyMembershipRule> rules = new ArrayList<>();
    private final PartyOptions options;
    private final Clock clock;

    private final PartyStore store;

    private PartyManager(@NotNull PartyOptions options, @NotNull Clock clock, @NotNull PartyStore store) {
        this.options = requireNonNull(options, "options");
        this.clock = requireNonNull(clock, "clock");
        this.store = requireNonNull(store, "store");
        this.rules.add(PartyMembershipRules.maxMembers(options.maxMembers()));
    }

    @NotNull
    public static PartyManager create() {
        return new PartyManager(PartyOptions.defaults(), Clock.systemUTC(), PartyStore.inMemory());
    }

    @NotNull
    public static PartyManager create(@NotNull PartyOptions options) {
        return new PartyManager(options, Clock.systemUTC(), PartyStore.inMemory());
    }

    @NotNull
    public static PartyManager create(@NotNull PartyOptions options, @NotNull Clock clock) {
        return new PartyManager(options, clock, PartyStore.inMemory());
    }

    /**
     * Creates a manager that writes every membership change through to the store.
     *
     * @param options membership options
     * @param clock   invite expiry clock
     * @param store   persistence; call {@link #load()} once after construction
     * @return the manager
     */
    @NotNull
    public static PartyManager create(@NotNull PartyOptions options, @NotNull Clock clock, @NotNull PartyStore store) {
        return new PartyManager(options, clock, store);
    }

    /**
     * The store this manager persists to.
     *
     * @return the party store
     */
    @NotNull
    public PartyStore store() {
        return this.store;
    }

    /**
     * Restores every persisted party. Parties whose members are already in a live party are
     * skipped. Safe to call once at startup before players join.
     *
     * @return number of parties restored
     */
    @NotNull
    public Promise<Integer> load() {
        return Promise.wrapFuture(this.store.loadAll().toCompletableFuture().thenApply(this::restore));
    }

    private synchronized int restore(Map<PartyId, PartySnapshot> snapshots) {
        int restored = 0;
        for (PartySnapshot snapshot : snapshots.values()) {
            PartyId id = snapshot.partyId();
            if (this.parties.containsKey(id) || snapshot.roles().keySet().stream().anyMatch(this.memberIndex::containsKey)) {
                continue;
            }
            PartyGroup party = PartyGroup.restore(snapshot);
            this.parties.put(id, party);
            party.members().forEach(member -> this.memberIndex.put(member, id));
            restored++;
        }
        return restored;
    }

    private void persist(@NotNull PartyGroup party) {
        this.store.save(party.id(), PartySnapshot.of(party)).exceptionallyAsync(error -> {
            RamLog.warn("failed to persist party " + party.id(), error);
            return null;
        });
    }

    private void forget(@NotNull PartyId id) {
        this.store.delete(id).exceptionallyAsync(error -> {
            RamLog.warn("failed to delete persisted party " + id, error);
            return null;
        });
    }

    @NotNull
    public synchronized PartyManager rule(@NotNull PartyMembershipRule rule) {
        this.rules.add(requireNonNull(rule, "rule"));
        return this;
    }

    @NotNull
    public synchronized PartyResult<PartyGroup> createParty(@NotNull UUID leader) {
        return createParty(PartyId.random(), leader);
    }

    @NotNull
    public synchronized PartyResult<PartyGroup> createParty(@NotNull PartyId id, @NotNull UUID leader) {
        requireNonNull(id, "id");
        requireNonNull(leader, "leader");
        if (this.parties.containsKey(id)) {
            return PartyResult.failure("party already exists");
        }
        if (this.memberIndex.containsKey(leader)) {
            return PartyResult.failure("leader is already in a party");
        }
        PartyGroup party = new PartyGroup(id, leader);
        this.parties.put(id, party);
        this.memberIndex.put(leader, id);
        persist(party);
        return PartyResult.ok(party);
    }

    @NotNull
    public synchronized Optional<PartyGroup> party(@NotNull PartyId id) {
        return Optional.ofNullable(this.parties.get(requireNonNull(id, "id")));
    }

    @NotNull
    public synchronized Optional<PartyGroup> partyOf(@NotNull UUID playerId) {
        PartyId id = this.memberIndex.get(requireNonNull(playerId, "playerId"));
        return id == null ? Optional.empty() : party(id);
    }

    @NotNull
    public synchronized List<PartyGroup> parties() {
        return List.copyOf(this.parties.values());
    }

    @NotNull
    public synchronized PartyResult<PartyInvite> invite(@NotNull UUID inviter, @NotNull UUID target) {
        requireNonNull(inviter, "inviter");
        requireNonNull(target, "target");
        Optional<PartyGroup> party = partyOf(inviter);
        if (party.isEmpty()) {
            return PartyResult.failure("inviter is not in a party");
        }
        return invite(party.get().id(), inviter, target);
    }

    @NotNull
    public synchronized PartyResult<PartyInvite> invite(@NotNull PartyId partyId, @NotNull UUID inviter, @NotNull UUID target) {
        PartyGroup party = this.parties.get(requireNonNull(partyId, "partyId"));
        if (party == null) {
            return PartyResult.failure("party does not exist");
        }
        if (!party.contains(inviter)) {
            return PartyResult.failure("inviter is not in the party");
        }
        if (this.memberIndex.containsKey(target)) {
            return PartyResult.failure("target is already in a party");
        }
        PartyResult<Void> allowed = validateJoin(party, target);
        if (!allowed.success()) {
            return PartyResult.failure(allowed.message());
        }
        Instant now = Instant.now(this.clock);
        PartyInvite invite = new PartyInvite(party.id(), target, inviter, now, now.plus(this.options.inviteTtl()));
        party.invite(invite);
        return PartyResult.ok(invite);
    }

    @NotNull
    public synchronized PartyResult<PartyGroup> accept(@NotNull PartyId partyId, @NotNull UUID playerId) {
        PartyGroup party = this.parties.get(requireNonNull(partyId, "partyId"));
        if (party == null) {
            return PartyResult.failure("party does not exist");
        }
        if (this.memberIndex.containsKey(playerId)) {
            return PartyResult.failure("player is already in a party");
        }
        PartyInvite invite = party.invite(playerId);
        if (invite == null) {
            return PartyResult.failure("player has no invite");
        }
        if (invite.expired(this.clock)) {
            party.removeInvite(playerId);
            return PartyResult.failure("invite has expired");
        }
        PartyResult<Void> allowed = validateJoin(party, playerId);
        if (!allowed.success()) {
            return PartyResult.failure(allowed.message());
        }
        party.removeInvite(playerId);
        party.addMember(playerId);
        this.memberIndex.put(playerId, party.id());
        persist(party);
        return PartyResult.ok(party);
    }

    @NotNull
    public synchronized PartyResult<PartyGroup> addMember(@NotNull PartyId partyId, @NotNull UUID playerId) {
        PartyGroup party = this.parties.get(requireNonNull(partyId, "partyId"));
        if (party == null) {
            return PartyResult.failure("party does not exist");
        }
        if (this.memberIndex.containsKey(playerId)) {
            return PartyResult.failure("player is already in a party");
        }
        PartyResult<Void> allowed = validateJoin(party, playerId);
        if (!allowed.success()) {
            return PartyResult.failure(allowed.message());
        }
        party.addMember(playerId);
        this.memberIndex.put(playerId, party.id());
        persist(party);
        return PartyResult.ok(party);
    }

    @NotNull
    public synchronized PartyResult<Void> leave(@NotNull UUID playerId) {
        PartyGroup party = partyOf(playerId).orElse(null);
        if (party == null) {
            return PartyResult.failure("player is not in a party");
        }
        if (party.size() == 1 || party.leader(playerId) && this.options.disbandWhenLeaderLeaves()) {
            disband(party.id());
            return PartyResult.ok();
        }
        if (party.leader(playerId)) {
            UUID nextLeader = party.members().stream()
                    .filter(member -> !member.equals(playerId))
                    .min(Comparator.comparing(UUID::toString))
                    .orElseThrow();
            party.promote(nextLeader);
        }
        party.removeMember(playerId);
        this.memberIndex.remove(playerId);
        persist(party);
        return PartyResult.ok();
    }

    @NotNull
    public synchronized PartyResult<Void> kick(@NotNull UUID actor, @NotNull UUID target) {
        PartyGroup party = partyOf(actor).orElse(null);
        if (party == null) {
            return PartyResult.failure("actor is not in a party");
        }
        if (!party.leader(actor)) {
            return PartyResult.failure("actor is not the party leader");
        }
        if (actor.equals(target)) {
            return PartyResult.failure("leader cannot kick self");
        }
        if (!party.contains(target)) {
            return PartyResult.failure("target is not in the party");
        }
        party.removeMember(target);
        this.memberIndex.remove(target);
        persist(party);
        return PartyResult.ok();
    }

    @NotNull
    public synchronized PartyResult<Void> promote(@NotNull UUID actor, @NotNull UUID target) {
        PartyGroup party = partyOf(actor).orElse(null);
        if (party == null) {
            return PartyResult.failure("actor is not in a party");
        }
        if (!party.leader(actor)) {
            return PartyResult.failure("actor is not the party leader");
        }
        if (!party.contains(target)) {
            return PartyResult.failure("target is not in the party");
        }
        party.promote(target);
        persist(party);
        return PartyResult.ok();
    }

    @NotNull
    public synchronized PartyResult<Void> disband(@NotNull PartyId partyId) {
        PartyGroup party = this.parties.remove(requireNonNull(partyId, "partyId"));
        if (party == null) {
            return PartyResult.failure("party does not exist");
        }
        party.members().forEach(this.memberIndex::remove);
        party.cleanup();
        forget(partyId);
        return PartyResult.ok();
    }

    public synchronized void cleanupInvites() {
        for (PartyGroup party : this.parties.values()) {
            party.invites().values().stream()
                    .filter(invite -> invite.expired(this.clock))
                    .map(PartyInvite::target)
                    .forEach(party::removeInvite);
        }
    }

    @NotNull
    private PartyResult<Void> validateJoin(@NotNull PartyGroup party, @NotNull UUID playerId) {
        for (PartyMembershipRule rule : this.rules) {
            PartyResult<Void> result = rule.test(party, playerId);
            if (!result.success()) {
                return result;
            }
        }
        return PartyResult.ok();
    }
}
