package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.party.PartyGroup;
import dev.willram.ramcore.party.PartyManager;
import dev.willram.ramcore.region.RegionRuleEngine;
import dev.willram.ramcore.region.RegionShape;
import dev.willram.ramcore.region.RegionTracker;
import dev.willram.ramcore.region.RuleRegion;
import dev.willram.ramcore.serialize.Position;
import dev.willram.ramcore.testkit.FakeClock;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StatServiceTest {
    private static final ContentId POWER = ContentId.parse("test:power");

    private static Player player(UUID id) {
        return ProxyFakes.proxy(Player.class, Map.of("getUniqueId", id));
    }

    private static StatRegistry registryWithPower() {
        StatRegistry registry = new StatRegistry();
        registry.register("test", Stat.of(POWER, 10.0D));
        return registry;
    }

    @Test
    public void snapshotCachesUntilInvalidated() {
        UUID id = UUID.randomUUID();
        Player player = player(id);
        StatService service = StatService.create(registryWithPower());

        AtomicReference<Double> amount = new AtomicReference<>(5.0D);
        service.addSource(p -> List.of(StatModifier.add(POWER, amount.get(), "src")));

        assertEquals(15.0D, service.snapshot(player).value(POWER));

        amount.set(20.0D);
        // cached — unchanged until invalidation
        assertEquals(15.0D, service.snapshot(player).value(POWER));

        service.invalidate(id);
        assertEquals(30.0D, service.snapshot(player).value(POWER));
    }

    @Test
    public void closedServiceReturnsEmpty() throws Exception {
        Player player = player(UUID.randomUUID());
        StatService service = StatService.create(registryWithPower());
        service.addSource(p -> List.of(StatModifier.add(POWER, 5.0D, "src")));
        service.close();
        assertTrue(service.snapshot(player).values().isEmpty());
    }

    @Test
    public void buffSourceExpiresAndNotifies() {
        UUID id = UUID.randomUUID();
        Player player = player(id);
        FakeClock clock = FakeClock.epoch();
        AtomicInteger changes = new AtomicInteger();
        BuffStatSource buffs = new BuffStatSource(clock, uuid -> changes.incrementAndGet());

        buffs.add(id, StatModifier.add(POWER, 4.0D, "buff:rage"), Duration.ofSeconds(10));
        assertEquals(1, changes.get());
        assertEquals(1, buffs.modifiers(player).size());

        clock.advance(Duration.ofSeconds(11));
        assertTrue(buffs.modifiers(player).isEmpty());

        buffs.add(id, StatModifier.add(POWER, 1.0D, "buff:perm"));
        buffs.clear(id);
        assertTrue(buffs.modifiers(player).isEmpty());
        buffs.close();
        assertTrue(buffs.isClosed());
    }

    @Test
    public void regionSourceReadsCurrentRegions() {
        UUID id = UUID.randomUUID();
        Player player = player(id);
        ContentId regionId = ContentId.parse("test:arena");

        RegionRuleEngine engine = new RegionRuleEngine();
        RegionShape everywhere = position -> true;
        engine.register("test", RuleRegion.builder(regionId, everywhere).build());
        RegionTracker tracker = RegionTracker.create(engine);
        tracker.transition(id, null, Position.of(0.0D, 0.0D, 0.0D, "world"));

        RegionStatSource source = new RegionStatSource(tracker);
        source.put(regionId, StatModifier.add(POWER, 7.0D, "region:arena"));

        assertEquals(1, source.modifiers(player).size());
        assertEquals(7.0D, source.modifiers(player).iterator().next().amount());
    }

    @Test
    public void partySourceUsesExtractor() {
        UUID id = UUID.randomUUID();
        Player player = player(id);
        PartyManager manager = PartyManager.create();
        manager.createParty(id);

        PartyStatSource source = new PartyStatSource(manager,
                (PartyGroup party) -> List.of(StatModifier.multiply(POWER, 0.25D, "party:" + party.size())));

        assertEquals(1, source.modifiers(player).size());
        assertEquals(0.25D, source.modifiers(player).iterator().next().amount());

        // player not in a party -> no modifiers
        assertTrue(source.modifiers(player(UUID.randomUUID())).isEmpty());
    }
}
