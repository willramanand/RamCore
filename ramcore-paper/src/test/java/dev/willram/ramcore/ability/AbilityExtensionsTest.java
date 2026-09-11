package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.testkit.FakeClock;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilityExtensionsTest {
    private static final ContentId BEAM = ContentId.parse("test:beam");
    private static final ContentId A = ContentId.parse("test:a");
    private static final ContentId B = ContentId.parse("test:b");
    private static final ContentId FINISHER = ContentId.parse("test:finisher");

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

    @Test
    public void channelTicksThenExecutes() {
        AtomicInteger ticks = new AtomicInteger();
        AtomicInteger executed = new AtomicInteger();
        Ability beam = Ability.builder(BEAM)
                .channel(new AbilityChannel(2L, 4L, (ctx, index) -> ticks.incrementAndGet()))
                .action(ctx -> executed.incrementAndGet())
                .build();
        AbilityCaster caster = AbilityCaster.create(this.player, null, this.clock);

        assertEquals(AbilityCastStatus.CASTING, caster.cast(beam, AbilityTrigger.HOTBAR).status());
        this.scheduler.tick(2);
        assertEquals(1, ticks.get());
        assertTrue(caster.casting());

        this.scheduler.tick(2);
        assertEquals(2, ticks.get());
        assertEquals(1, executed.get());
        assertFalse(caster.casting());
    }

    @Test
    public void channelCanBeInterrupted() {
        AtomicInteger ticks = new AtomicInteger();
        AtomicInteger executed = new AtomicInteger();
        Ability beam = Ability.builder(BEAM)
                .channel(new AbilityChannel(2L, 6L, (ctx, index) -> ticks.incrementAndGet()))
                .action(ctx -> executed.incrementAndGet())
                .build();
        AbilityCaster caster = AbilityCaster.create(this.player, null, this.clock);

        caster.cast(beam, AbilityTrigger.HOTBAR);
        this.scheduler.tick(2);
        assertEquals(1, ticks.get());

        assertTrue(caster.interrupt());
        assertFalse(caster.casting());
        this.scheduler.tick(6);
        assertEquals(1, ticks.get()); // no more ticks
        assertEquals(0, executed.get()); // action never ran
    }

    @Test
    public void serviceInterruptIfCastingStopsChannel() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register("test", Ability.builder(BEAM)
                .channel(new AbilityChannel(2L, 6L, (ctx, index) -> {
                }))
                .build());
        AbilityService service = AbilityService.create(registry, null, this.clock);

        service.cast(this.player, BEAM, AbilityTrigger.COMMAND);
        assertTrue(service.caster(this.player).casting());
        assertTrue(service.interruptIfCasting(this.player));
        assertFalse(service.caster(this.player).casting());
    }

    @Test
    public void comboTrackerMatchesWithinWindow() {
        ComboTracker tracker = new ComboTracker(this.clock);
        tracker.register(new AbilityCombo(java.util.List.of(A, B), Duration.ofSeconds(1), FINISHER));
        UUID id = this.player.getUniqueId();

        assertTrue(tracker.record(id, A).isEmpty());
        Optional<AbilityCombo> matched = tracker.record(id, B);
        assertTrue(matched.isPresent());
        assertEquals(FINISHER, matched.get().finisher());
    }

    @Test
    public void comboTrackerMissesOutsideWindow() {
        ComboTracker tracker = new ComboTracker(this.clock);
        tracker.register(new AbilityCombo(java.util.List.of(A, B), Duration.ofSeconds(1), FINISHER));
        UUID id = this.player.getUniqueId();

        tracker.record(id, A);
        this.clock.advance(Duration.ofSeconds(2));
        assertTrue(tracker.record(id, B).isEmpty());
    }

    @Test
    public void serviceFiresComboFinisher() {
        AtomicInteger finisherRuns = new AtomicInteger();
        AbilityRegistry registry = new AbilityRegistry();
        registry.register("test", Ability.builder(A).action(ctx -> {
        }).build());
        registry.register("test", Ability.builder(B).action(ctx -> {
        }).build());
        registry.register("test", Ability.builder(FINISHER).action(ctx -> finisherRuns.incrementAndGet()).build());

        AbilityService service = AbilityService.create(registry, null, this.clock);
        service.registerCombo(new AbilityCombo(java.util.List.of(A, B), Duration.ofSeconds(1), FINISHER));

        service.cast(this.player, A, AbilityTrigger.COMMAND);
        service.cast(this.player, B, AbilityTrigger.COMMAND);
        assertEquals(1, finisherRuns.get());
    }
}
