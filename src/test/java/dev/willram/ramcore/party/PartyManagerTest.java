package dev.willram.ramcore.party;

import dev.willram.ramcore.metadata.MetadataKey;
import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class PartyManagerTest {

    @Test
    public void createsPartyAndIndexesLeader() {
        PartyManager manager = Parties.manager();
        UUID leader = UUID.randomUUID();

        PartyResult<PartyGroup> result = manager.createParty(PartyId.of("test:alpha"), leader);

        assertTrue(result.success());
        assertEquals(PartyId.of("test:alpha"), result.value().id());
        assertEquals(leader, result.value().leader());
        assertTrue(result.value().contains(leader));
        assertSame(result.value(), manager.partyOf(leader).orElseThrow());
    }

    @Test
    public void inviteAcceptAddsMemberAndConsumesInvite() {
        PartyManager manager = Parties.manager();
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        PartyGroup party = manager.createParty(leader).value();

        PartyResult<PartyInvite> invite = manager.invite(leader, member);
        PartyResult<PartyGroup> accepted = manager.accept(party.id(), member);

        assertTrue(invite.success());
        assertTrue(accepted.success());
        assertTrue(party.contains(member));
        assertFalse(party.hasInvite(member));
        assertSame(party, manager.partyOf(member).orElseThrow());
    }

    @Test
    public void expiredInviteCannotBeAccepted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-07T00:00:00Z"));
        PartyManager manager = Parties.manager(PartyOptions.defaults().inviteTtl(Duration.ofMillis(1)), clock);
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        PartyGroup party = manager.createParty(leader).value();

        manager.invite(leader, member);
        clock.advance(Duration.ofMillis(2));
        PartyResult<PartyGroup> accepted = manager.accept(party.id(), member);

        assertFalse(accepted.success());
        assertEquals("invite has expired", accepted.message());
        assertFalse(party.contains(member));
    }

    @Test
    public void maxMemberRuleRejectsFullParty() {
        PartyManager manager = Parties.manager(PartyOptions.defaults().maxMembers(2));
        UUID leader = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PartyGroup party = manager.createParty(leader).value();

        assertTrue(manager.addMember(party.id(), first).success());
        PartyResult<PartyInvite> invite = manager.invite(leader, second);

        assertFalse(invite.success());
        assertEquals("party is full", invite.message());
    }

    @Test
    public void customMembershipRuleCanRejectPlayers() {
        UUID blocked = UUID.randomUUID();
        PartyManager manager = Parties.manager()
                .rule((party, playerId) -> playerId.equals(blocked)
                        ? PartyResult.failure("blocked")
                        : PartyResult.ok());
        UUID leader = UUID.randomUUID();
        PartyGroup party = manager.createParty(leader).value();

        PartyResult<PartyGroup> added = manager.addMember(party.id(), blocked);

        assertFalse(added.success());
        assertEquals("blocked", added.message());
    }

    @Test
    public void leaderLeaveCanPromoteNextMember() {
        PartyManager manager = Parties.manager(PartyOptions.defaults().disbandWhenLeaderLeaves(false));
        UUID leader = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID next = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PartyGroup party = manager.createParty(leader).value();
        manager.addMember(party.id(), other);
        manager.addMember(party.id(), next);

        PartyResult<Void> left = manager.leave(leader);

        assertTrue(left.success());
        assertEquals(next, party.leader());
        assertFalse(party.contains(leader));
        assertTrue(party.contains(other));
        assertTrue(manager.partyOf(leader).isEmpty());
    }

    @Test
    public void leaderLeaveDisbandsByDefault() {
        PartyManager manager = Parties.manager();
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        PartyGroup party = manager.createParty(leader).value();
        manager.addMember(party.id(), member);

        PartyResult<Void> left = manager.leave(leader);

        assertTrue(left.success());
        assertTrue(manager.party(party.id()).isEmpty());
        assertTrue(manager.partyOf(member).isEmpty());
    }

    @Test
    public void contributionsTrackEligibilityAndRanking() {
        PartyContributionTracker contributions = new PartyContributionTracker();
        UUID low = UUID.randomUUID();
        UUID high = UUID.randomUUID();

        contributions.add(low, 5.0d);
        contributions.add(high, 10.0d);
        contributions.add(high, 2.5d);

        assertEquals(17.5d, contributions.total(), 0.0d);
        assertEquals(Set.of(high), contributions.eligible(10.0d));
        assertEquals(List.of(high, low), List.copyOf(contributions.top(2).keySet()));
    }

    @Test
    public void partyExposesMetadataAndRewardContext() {
        MetadataKey<String> dungeon = MetadataKey.create("dungeon", String.class);
        PartyGroup party = Parties.manager().createParty(UUID.randomUUID()).value();

        party.metadata().put(dungeon, "crypt");

        assertEquals("crypt", party.metadata().getOrNull(dungeon));
        assertSame(party, party.rewardContext("loot").subject());
        assertEquals(party.id().toString(), party.rewardContext("loot").metadata().get("partyId"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.instant;
        }
    }
}
