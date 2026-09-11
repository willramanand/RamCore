package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilityServiceTest {
    private static final ContentId FIREBALL = ContentId.parse("test:fireball");
    private static final ContentId UNKNOWN = ContentId.parse("test:unknown");

    private FakeScheduler scheduler;
    private Player player;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.player = ProxyFakes.proxy(Player.class, Map.of("getUniqueId", UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    private AbilityService serviceWithFireball(AtomicInteger runs) {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register("test", Ability.builder(FIREBALL).action(ctx -> runs.incrementAndGet()).build());
        return AbilityService.create(registry);
    }

    @Test
    public void hotbarBindingCasts() {
        AtomicInteger runs = new AtomicInteger();
        AbilityService service = serviceWithFireball(runs);
        service.bindHotbar(3, FIREBALL);

        assertTrue(service.handleHotbar(this.player, 3).isPresent());
        assertEquals(AbilityCastStatus.CAST, service.handleHotbar(this.player, 3).map(CastResult::status).orElseThrow());
        // slot 5 unbound -> nothing
        assertTrue(service.handleHotbar(this.player, 5).isEmpty());
    }

    @Test
    public void swapBindingCasts() {
        AtomicInteger runs = new AtomicInteger();
        AbilityService service = serviceWithFireball(runs);
        service.bindSwapHands(FIREBALL);

        assertTrue(service.handleSwapHands(this.player).map(CastResult::started).orElse(false));
        assertTrue(runs.get() >= 1);
    }

    @Test
    public void castUnknownAbilityThrows() {
        AbilityService service = serviceWithFireball(new AtomicInteger());
        assertThrows(RuntimeException.class, () -> service.cast(this.player, UNKNOWN, AbilityTrigger.COMMAND));
    }

    @Test
    public void removeCasterClearsState() {
        AtomicInteger runs = new AtomicInteger();
        AbilityService service = serviceWithFireball(runs);
        AbilityCaster caster = service.caster(this.player);
        service.removeCaster(this.player.getUniqueId());
        assertTrue(caster.isClosed());
    }

    @Test
    public void hotbarSlotValidated() {
        AbilityService service = serviceWithFireball(new AtomicInteger());
        assertThrows(RuntimeException.class, () -> service.bindHotbar(9, FIREBALL));
    }

    @Test
    public void unboundHotbarIsNoop() {
        AbilityService service = serviceWithFireball(new AtomicInteger());
        assertFalse(service.handleHotbar(this.player, 0).isPresent());
    }
}
