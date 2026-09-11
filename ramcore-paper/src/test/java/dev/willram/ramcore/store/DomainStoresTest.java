package dev.willram.ramcore.store;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.cooldown.Cooldown;
import dev.willram.ramcore.cooldown.CooldownKey;
import dev.willram.ramcore.cooldown.CooldownSnapshot;
import dev.willram.ramcore.cooldown.CooldownStore;
import dev.willram.ramcore.cooldown.CooldownTracker;
import dev.willram.ramcore.loot.InstancedLoot;
import dev.willram.ramcore.loot.LootClaimPolicy;
import dev.willram.ramcore.loot.LootInstance;
import dev.willram.ramcore.loot.LootInstanceSnapshot;
import dev.willram.ramcore.loot.LootPayloadCodec;
import dev.willram.ramcore.loot.LootReward;
import dev.willram.ramcore.loot.PersistentLootInstanceStore;
import dev.willram.ramcore.objective.ObjectiveAction;
import dev.willram.ramcore.objective.ObjectiveDefinition;
import dev.willram.ramcore.objective.ObjectiveEvent;
import dev.willram.ramcore.objective.ObjectiveProgressKey;
import dev.willram.ramcore.objective.ObjectiveProgressSnapshot;
import dev.willram.ramcore.objective.ObjectiveProgressStore;
import dev.willram.ramcore.objective.ObjectiveSubject;
import dev.willram.ramcore.objective.ObjectiveTask;
import dev.willram.ramcore.objective.ObjectiveTracker;
import dev.willram.ramcore.party.PartyId;
import dev.willram.ramcore.party.PartyManager;
import dev.willram.ramcore.party.PartyOptions;
import dev.willram.ramcore.party.PartyRole;
import dev.willram.ramcore.party.PartySnapshot;
import dev.willram.ramcore.party.PartyStore;
import dev.willram.ramcore.testkit.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Domain stores over the in-memory backend: every promise completes synchronously, so no
 * scheduler is needed and behaviour without persistence is provably unchanged.
 */
public final class DomainStoresTest {

    @Test
    public void partyManagerWritesThroughAndReloads() {
        InMemoryStore<PartyId, PartySnapshot> backend = Stores.inMemory();
        PartyManager manager = PartyManager.create(PartyOptions.defaults(), FakeClock.epoch(), PartyStore.of(backend));
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        PartyId id = PartyId.of("raid");

        manager.createParty(id, leader);
        manager.addMember(id, member);

        PartySnapshot saved = backend.load(id).join().orElseThrow();
        assertEquals(leader, saved.leader());
        assertEquals(Map.of(leader, PartyRole.LEADER, member, PartyRole.MEMBER), saved.roles());

        PartyManager restarted = PartyManager.create(PartyOptions.defaults(), FakeClock.epoch(), PartyStore.of(backend));
        assertEquals(1, restarted.load().join());
        assertTrue(restarted.partyOf(member).isPresent());
        assertEquals(leader, restarted.party(id).orElseThrow().leader());

        restarted.promote(leader, member);
        assertEquals(member, backend.load(id).join().orElseThrow().leader());

        restarted.disband(id);
        assertEquals(Optional.empty(), backend.load(id).join());
    }

    @Test
    public void partyLoadSkipsPartiesThatCollideWithLiveMembers() {
        InMemoryStore<PartyId, PartySnapshot> backend = Stores.inMemory();
        UUID leader = UUID.randomUUID();
        backend.save(PartyId.of("stale"), new PartySnapshot("stale", leader, Map.of(leader, PartyRole.LEADER)));
        PartyManager manager = PartyManager.create(PartyOptions.defaults(), FakeClock.epoch(), PartyStore.of(backend));
        manager.createParty(PartyId.of("live"), leader);

        assertEquals(0, manager.load().join());
        assertEquals(PartyId.of("live"), manager.partyOf(leader).orElseThrow().id());
    }

    @Test
    public void cooldownTrackerPersistsConsumedCooldownsAndRestoresActiveOnes() {
        InMemoryStore<String, CooldownSnapshot> backend = Stores.inMemory();
        CooldownTracker<String> tracker = CooldownTracker.create(Cooldown.of(10, TimeUnit.MINUTES), CooldownStore.of(backend));

        assertTrue(tracker.test("cast").allowed());
        CooldownSnapshot saved = backend.load("cast").join().orElseThrow();
        assertEquals(TimeUnit.MINUTES.toMillis(10), saved.timeoutMillis());
        assertFalse(saved.expiredAt(System.currentTimeMillis()));

        backend.save("old", new CooldownSnapshot(1L, TimeUnit.MINUTES.toMillis(10)));
        CooldownTracker<String> restarted = CooldownTracker.create(Cooldown.of(10, TimeUnit.MINUTES), CooldownStore.of(backend));
        assertEquals(1, restarted.load().join(), "only the still-active cooldown is restored");
        assertTrue(restarted.active("cast"));
        assertEquals(Optional.empty(), backend.load("old").join(), "expired entries are deleted on load");

        restarted.remove("cast");
        assertEquals(Optional.empty(), backend.load("cast").join());
    }

    @Test
    public void cooldownKeyCodecRoundTripsUuidAndStringKeys() {
        var codec = CooldownKey.keyCodec();
        UUID player = UUID.randomUUID();

        assertEquals(CooldownKey.uuid("combat", player), codec.decode(codec.encode(CooldownKey.uuid("combat", player))));
        assertEquals(CooldownKey.of("warp", "spawn"), codec.decode(codec.encode(CooldownKey.of("warp", "spawn"))));
    }

    @Test
    public void objectiveTrackerPersistsProgressAndReloadsIt() {
        InMemoryStore<ObjectiveProgressKey, ObjectiveProgressSnapshot> backend = Stores.inMemory();
        ContentId objectiveId = ContentId.of("quests", "hunt");
        ObjectiveDefinition definition = ObjectiveDefinition.builder(objectiveId)
                .task(ObjectiveTask.of("kill", ObjectiveAction.KILL, "zombie", 5))
                .build();
        ObjectiveTracker tracker = ObjectiveTracker.create(ObjectiveProgressStore.of(backend)).register(definition);
        ObjectiveSubject subject = ObjectiveSubject.player(UUID.randomUUID());

        tracker.apply(ObjectiveEvent.of(subject, ObjectiveAction.KILL, "zombie").amount(2));

        ObjectiveProgressKey key = new ObjectiveProgressKey(subject, objectiveId);
        assertEquals(Map.of("kill", 2L), backend.load(key).join().orElseThrow().amounts());

        ObjectiveTracker restarted = ObjectiveTracker.create(ObjectiveProgressStore.of(backend)).register(definition);
        assertEquals(1, restarted.load().join());
        assertEquals(2, restarted.progress(subject, objectiveId).current("kill"));

        restarted.reset(subject, objectiveId);
        assertEquals(Optional.empty(), backend.load(key).join());
    }

    @Test
    public void objectiveProgressKeyCodecRoundTrips() {
        var codec = ObjectiveProgressKey.keyCodec();
        ObjectiveProgressKey key = new ObjectiveProgressKey(ObjectiveSubject.party(PartyId.of("raid")), ContentId.of("quests", "hunt"));

        assertEquals("party/raid/quests:hunt", codec.encode(key));
        assertEquals(key, codec.decode("party/raid/quests:hunt"));
    }

    @Test
    public void persistentLootStoreWritesThroughAndReloadsClaims() {
        InMemoryStore<UUID, LootInstanceSnapshot> backend = Stores.inMemory();
        PersistentLootInstanceStore store = InstancedLoot.persistentStore(backend, LootPayloadCodec.strings());
        UUID claimant = UUID.randomUUID();
        LootInstance instance = InstancedLoot.instance(ContentId.of("loot", "chest"), List.of(LootReward.of("gold", "100", 2)))
                .claimPolicy(LootClaimPolicy.PER_PLAYER_ONCE)
                .expiresAt(Instant.now().plusSeconds(3600))
                .metadata("source", "boss")
                .build();

        store.register(instance);
        assertTrue(store.claim(instance.id(), claimant, Instant.now()).successful());

        LootInstanceSnapshot saved = backend.load(instance.id()).join().orElseThrow();
        assertEquals(java.util.Set.of(claimant), saved.claimedBy());
        assertEquals("100", saved.rewards().getFirst().payload());

        PersistentLootInstanceStore restarted = InstancedLoot.persistentStore(backend, LootPayloadCodec.strings());
        assertEquals(1, restarted.load().join());
        LootInstance restored = restarted.get(instance.id()).orElseThrow();
        assertEquals("boss", restored.metadata().get("source"));
        assertFalse(restarted.claim(instance.id(), claimant, Instant.now()).successful(), "claims survive the reload");

        restarted.remove(instance.id());
        assertEquals(Optional.empty(), backend.load(instance.id()).join());
    }

    @Test
    public void expiredLootInstancesAreDroppedOnLoad() {
        InMemoryStore<UUID, LootInstanceSnapshot> backend = Stores.inMemory();
        PersistentLootInstanceStore store = InstancedLoot.persistentStore(backend, LootPayloadCodec.strings());
        LootInstance expired = InstancedLoot.instance(ContentId.of("loot", "chest"), List.of(LootReward.of("gold")))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        store.register(expired);

        PersistentLootInstanceStore restarted = InstancedLoot.persistentStore(backend, LootPayloadCodec.strings());
        assertEquals(0, restarted.load().join());
        assertEquals(Optional.empty(), backend.load(expired.id()).join());
    }
}
