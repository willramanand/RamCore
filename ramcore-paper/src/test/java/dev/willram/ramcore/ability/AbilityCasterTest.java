package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.stat.Stat;
import dev.willram.ramcore.stat.StatModifier;
import dev.willram.ramcore.stat.StatRegistry;
import dev.willram.ramcore.stat.StatService;
import dev.willram.ramcore.testkit.FakeClock;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilityCasterTest {
    private static final ContentId FIREBALL = ContentId.parse("test:fireball");
    private static final ContentId MANA = ContentId.parse("test:mana");

    private FakeScheduler scheduler;
    private FakeClock clock;
    private Player player;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.clock = FakeClock.epoch();
        this.player = ProxyFakes.proxy(Player.class, Map.of("getUniqueId", UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    private AbilityCaster caster() {
        return AbilityCaster.create(this.player, null, this.clock);
    }

    private AbilityCaster caster(StatService service) {
        return AbilityCaster.create(this.player, service, this.clock);
    }

    @Test
    public void instantCastExecutesAndStartsCooldown() {
        AtomicInteger runs = new AtomicInteger();
        Ability ability = Ability.builder(FIREBALL)
                .cooldown(Duration.ofSeconds(5))
                .action(ctx -> runs.incrementAndGet())
                .build();
        AbilityCaster caster = caster();

        CastResult first = caster.cast(ability, AbilityTrigger.COMMAND);
        assertEquals(AbilityCastStatus.CAST, first.status());
        assertEquals(1, runs.get());

        CastResult second = caster.cast(ability, AbilityTrigger.COMMAND);
        assertEquals(AbilityCastStatus.ON_COOLDOWN, second.status());
        assertEquals(5000L, second.remainingMillis());
        assertEquals(1, runs.get());

        this.clock.advance(Duration.ofSeconds(5));
        CastResult third = caster.cast(ability, AbilityTrigger.COMMAND);
        assertEquals(AbilityCastStatus.CAST, third.status());
        assertEquals(2, runs.get());
    }

    @Test
    public void channelledCastExecutesAfterCastTime() {
        AtomicInteger runs = new AtomicInteger();
        Ability ability = Ability.builder(FIREBALL)
                .castTicks(2)
                .action(ctx -> runs.incrementAndGet())
                .build();
        AbilityCaster caster = caster();

        CastResult result = caster.cast(ability, AbilityTrigger.HOTBAR);
        assertEquals(AbilityCastStatus.CASTING, result.status());
        assertTrue(caster.casting());
        assertEquals(0, runs.get());

        // busy while channelling
        assertEquals(AbilityCastStatus.BUSY, caster.cast(ability, AbilityTrigger.HOTBAR).status());

        this.scheduler.tick(2);
        assertEquals(1, runs.get());
        assertFalse(caster.casting());
    }

    @Test
    public void interruptCancelsChannel() {
        AtomicInteger runs = new AtomicInteger();
        Ability ability = Ability.builder(FIREBALL)
                .castTicks(2)
                .action(ctx -> runs.incrementAndGet())
                .build();
        AbilityCaster caster = caster();

        caster.cast(ability, AbilityTrigger.HOTBAR);
        assertTrue(caster.interrupt());
        assertFalse(caster.casting());

        this.scheduler.tick(2);
        assertEquals(0, runs.get());
        assertFalse(caster.interrupt()); // nothing to interrupt now
    }

    @Test
    public void costGateBlocksWhenTooPoor() {
        StatRegistry registry = new StatRegistry();
        registry.register("test", Stat.of(MANA, 0.0D));
        StatService service = StatService.create(registry);
        service.addSource(p -> List.of(StatModifier.add(MANA, 10.0D, "s")));

        Ability ability = Ability.builder(FIREBALL).cost(MANA, 30.0D)
                .action(ctx -> {
                }).build();

        assertEquals(AbilityCastStatus.INSUFFICIENT_COST, caster(service).cast(ability, AbilityTrigger.COMMAND).status());
    }

    @Test
    public void costGateAllowsWhenRich() {
        StatRegistry registry = new StatRegistry();
        registry.register("test", Stat.of(MANA, 0.0D));
        StatService service = StatService.create(registry);
        service.addSource(p -> List.of(StatModifier.add(MANA, 50.0D, "s")));

        Ability ability = Ability.builder(FIREBALL).cost(MANA, 30.0D)
                .action(ctx -> {
                }).build();

        assertEquals(AbilityCastStatus.CAST, caster(service).cast(ability, AbilityTrigger.COMMAND).status());
    }

    @Test
    public void validationErrorsBlockCast() {
        AtomicInteger runs = new AtomicInteger();
        Ability ability = Ability.builder(FIREBALL)
                .action(new AbilityAction() {
                    @Override
                    public void run(dev.willram.ramcore.ability.AbilityContext context) {
                        runs.incrementAndGet();
                    }

                    @Override
                    public List<String> validate(dev.willram.ramcore.ability.AbilityContext context) {
                        return List.of("no target");
                    }
                })
                .build();

        CastResult result = caster().cast(ability, AbilityTrigger.COMMAND);
        assertEquals(AbilityCastStatus.INVALID, result.status());
        assertEquals(List.of("no target"), result.errors());
        assertEquals(0, runs.get());
    }

    @Test
    public void telegraphIsShownWhileCastingAndClearedOnCompletion() {
        AtomicInteger shows = new AtomicInteger();
        AtomicInteger clears = new AtomicInteger();
        Ability ability = Ability.builder(FIREBALL)
                .castTicks(2)
                .telegraph(caster -> {
                    shows.incrementAndGet();
                    return clears::incrementAndGet;
                })
                .action(ctx -> {
                })
                .build();
        AbilityCaster caster = caster();

        caster.cast(ability, AbilityTrigger.HOTBAR);
        assertEquals(1, shows.get());
        assertEquals(0, clears.get());

        this.scheduler.tick(2);
        assertEquals(1, clears.get());
    }

    @Test
    public void telegraphIsClearedOnInterrupt() {
        AtomicInteger clears = new AtomicInteger();
        Ability ability = Ability.builder(FIREBALL)
                .castTicks(4)
                .telegraph(caster -> clears::incrementAndGet)
                .action(ctx -> {
                })
                .build();
        AbilityCaster caster = caster();

        caster.cast(ability, AbilityTrigger.HOTBAR);
        assertEquals(0, clears.get());
        caster.interrupt();
        assertEquals(1, clears.get());
    }
}
