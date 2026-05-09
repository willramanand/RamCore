package dev.willram.ramcore.loot;

import dev.willram.ramcore.content.ContentId;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InstancedLootTest {
    private static final ContentId TABLE_ID = ContentId.parse("test:chest");

    @Test
    public void generatorRollsGuaranteedWeightedAndBonusRewards() {
        LootTable table = LootTable.builder(TABLE_ID)
                .guaranteed(LootEntry.guaranteed("coin", LootReward.of("coin", "gold", 5)))
                .weighted(LootEntry.weighted("gem", 1, LootReward.of("gem")))
                .rolls(1)
                .bonusRolls(context -> (int) context.luck())
                .build();

        LootGenerationResult result = new LootGenerator().generate(table, LootContext.builder("boss").luck(2).build(), new Random(1));

        assertTrue(result.successful());
        assertEquals(TABLE_ID, result.tableId());
        assertEquals(List.of("coin", "gem", "gem", "gem"), result.rewards().stream().map(LootReward::id).toList());
    }

    @Test
    public void perPlayerClaimPolicyBlocksDuplicateClaimsOnlyForSamePlayer() {
        LootInstanceStore store = InstancedLoot.inMemoryStore();
        LootInstance instance = store.register(LootInstance.builder(TABLE_ID, List.of(LootReward.of("coin")))
                .claimPolicy(LootClaimPolicy.PER_PLAYER_ONCE)
                .build());
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-08T12:00:00Z");

        assertTrue(store.claim(instance.id(), first, now).successful());
        assertEquals(LootClaimStatus.ALREADY_CLAIMED, store.claim(instance.id(), first, now).status());
        assertTrue(store.claim(instance.id(), second, now).successful());
        assertEquals(2, instance.claimedBy().size());
    }

    @Test
    public void singleClaimPolicyDepletesInstanceForEveryone() {
        LootInstanceStore store = InstancedLoot.inMemoryStore();
        LootInstance instance = store.register(LootInstance.builder(TABLE_ID, List.of(LootReward.of("key")))
                .scope(LootInstanceScope.GLOBAL)
                .claimPolicy(LootClaimPolicy.SINGLE_CLAIM)
                .build());
        Instant now = Instant.parse("2026-05-08T12:00:00Z");

        assertTrue(store.claim(instance.id(), UUID.randomUUID(), now).successful());
        assertEquals(LootClaimStatus.ALREADY_CLAIMED, store.claim(instance.id(), UUID.randomUUID(), now).status());
    }

    @Test
    public void expiredInstancesCannotBeClaimedAndCanBeSwept() {
        LootInstanceStore store = InstancedLoot.inMemoryStore();
        AtomicInteger expired = new AtomicInteger();
        store.addListener(new LootInstanceListener() {
            @Override
            public void expired(LootInstance instance) {
                expired.incrementAndGet();
            }
        });
        Instant now = Instant.parse("2026-05-08T12:00:00Z");
        LootInstance instance = store.register(LootInstance.builder(TABLE_ID, List.of(LootReward.of("coin")))
                .expiresAt(now.plus(Duration.ofSeconds(5)))
                .build());

        assertEquals(LootClaimStatus.EXPIRED, store.claim(instance.id(), UUID.randomUUID(), now.plusSeconds(5)).status());
        assertEquals(List.of(instance), store.sweepExpired(now.plusSeconds(6)));
        assertEquals(1, expired.get());
        assertFalse(store.get(instance.id()).isPresent());
    }

    @Test
    public void rerollCanReplaceRewardsAndClearClaims() {
        LootInstanceStore store = InstancedLoot.inMemoryStore();
        AtomicInteger rerolled = new AtomicInteger();
        store.addListener(new LootInstanceListener() {
            @Override
            public void rerolled(LootInstance instance, List<LootReward> previousRewards) {
                assertEquals(List.of("coin"), previousRewards.stream().map(LootReward::id).toList());
                rerolled.incrementAndGet();
            }
        });
        LootInstance instance = store.register(LootInstance.builder(TABLE_ID, List.of(LootReward.of("coin"))).build());
        UUID player = UUID.randomUUID();

        assertTrue(store.claim(instance.id(), player, Instant.now()).successful());
        store.reroll(instance.id(), List.of(LootReward.of("gem")), true);

        assertTrue(instance.claimedBy().isEmpty());
        assertEquals(List.of("gem"), instance.rewards().stream().map(LootReward::id).toList());
        assertEquals(1, rerolled.get());
    }
}
