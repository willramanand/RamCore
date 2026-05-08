package dev.willram.ramcore.encounter;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.region.RegionShapes;
import dev.willram.ramcore.serialize.Position;
import dev.willram.ramcore.serialize.Region;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class EncounterInstanceTest {

    @Test
    public void damageTracksContributorsAndCompletesEncounter() {
        UUID player = UUID.randomUUID();
        EncounterInstance encounter = instance();

        encounter.start();
        encounter.damage(player, 125.0d);

        assertEquals(EncounterState.COMPLETED, encounter.state());
        assertEquals(0.0d, encounter.health(), 0.0d);
        assertEquals(125.0d, encounter.contributions().get(player), 0.0d);
        assertEquals(encounter, encounter.rewardContext("boss").subject());
        assertEquals("example:boss", encounter.rewardContext("boss").metadata().get("encounterId"));
    }

    @Test
    public void phaseChangesAtHealthThreshold() {
        EncounterInstance encounter = instance();

        encounter.start();
        encounter.damage(UUID.randomUUID(), 51.0d);

        assertEquals("burn", encounter.phase().id());
    }

    @Test
    public void abilitiesRunOnPhaseInterval() {
        AtomicInteger casts = new AtomicInteger();
        EncounterDefinition definition = Encounters.encounter(ContentId.parse("example:caster"), 100.0d)
                .phase(Encounters.phase("main", 1.0d)
                        .ability(Encounters.ability("blast", 2).initialDelay(1).action((encounter, ability) -> casts.incrementAndGet())))
                .build();
        EncounterInstance encounter = new EncounterInstance(definition);
        List<EncounterSignal> signals = new ArrayList<>();
        encounter.listener(update -> signals.add(update.signal()));

        encounter.start();
        encounter.tick();
        encounter.tick();
        encounter.tick();

        assertEquals(2, casts.get());
        assertEquals(2, signals.stream().filter(signal -> signal == EncounterSignal.ABILITY).count());
    }

    @Test
    public void encounterEnragesAfterConfiguredTicks() {
        EncounterDefinition definition = Encounters.encounter(ContentId.parse("example:enrage"), 100.0d)
                .enrageAfterTicks(3)
                .phase(Encounters.phase("main", 1.0d))
                .build();
        EncounterInstance encounter = new EncounterInstance(definition);

        encounter.start();
        encounter.tick();
        encounter.tick();
        assertEquals(EncounterState.RUNNING, encounter.state());
        encounter.tick();

        assertEquals(EncounterState.ENRAGED, encounter.state());
    }

    @Test
    public void arenaChecksPositionContainment() {
        EncounterDefinition definition = Encounters.encounter(ContentId.parse("example:arena"), 100.0d)
                .arena(RegionShapes.cuboid(Region.of(
                        Position.of(0.0d, 0.0d, 0.0d, "world"),
                        Position.of(10.0d, 10.0d, 10.0d, "world")
                )))
                .phase(Encounters.phase("main", 1.0d))
                .build();
        EncounterInstance encounter = new EncounterInstance(definition);

        assertTrue(encounter.withinArena(Position.of(5.0d, 5.0d, 5.0d, "world")));
        assertFalse(encounter.withinArena(Position.of(20.0d, 5.0d, 5.0d, "world")));
    }

    @Test
    public void resetRestoresHealthPhaseTicksAndContributions() {
        UUID player = UUID.randomUUID();
        EncounterInstance encounter = instance();

        encounter.start();
        encounter.tick();
        encounter.damage(player, 60.0d);
        encounter.reset("manual");

        assertEquals(EncounterState.RESET, encounter.state());
        assertEquals(100.0d, encounter.health(), 0.0d);
        assertEquals("main", encounter.phase().id());
        assertEquals(0L, encounter.elapsedTicks());
        assertEquals(0.0d, encounter.contributions().get(player), 0.0d);
    }

    @Test
    public void registryCreatesInstancesWithSharedListeners() {
        List<EncounterSignal> signals = new ArrayList<>();
        EncounterRegistry registry = Encounters.registry(update -> signals.add(update.signal()));
        EncounterDefinition definition = Encounters.encounter(ContentId.parse("example:registered"), 100.0d)
                .phase(Encounters.phase("main", 1.0d))
                .build();

        EncounterInstance encounter = registry.register(definition).create(definition.id());
        encounter.start();

        assertSame(definition, encounter.definition());
        assertEquals(List.of(EncounterSignal.START), signals);
    }

    private static EncounterInstance instance() {
        EncounterDefinition definition = Encounters.encounter(ContentId.parse("example:boss"), 100.0d)
                .phase(Encounters.phase("main", 1.0d))
                .phase(Encounters.phase("burn", 0.5d))
                .build();
        return new EncounterInstance(definition);
    }
}
