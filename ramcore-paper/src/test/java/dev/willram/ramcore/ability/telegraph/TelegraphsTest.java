package dev.willram.ramcore.ability.telegraph;

import dev.willram.ramcore.ability.AbilityTargeting;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TelegraphsTest {

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

    @Test
    public void particleTelegraphsRejectBadArgs() {
        assertThrows(RuntimeException.class, () -> Telegraphs.ring(0.0D));
        assertThrows(RuntimeException.class, () -> Telegraphs.line(-1.0D));
        assertThrows(RuntimeException.class, () -> Telegraphs.cone(10.0D, 0.0D));
        assertThrows(RuntimeException.class, () -> Telegraphs.cone(10.0D, 181.0D));
        assertThrows(RuntimeException.class, () -> Telegraphs.ring(3.0D, Particle.END_ROD, 0L));
    }

    @Test
    public void ringSchedulesAndClosesWithoutDrawingForInvalidCaster() throws Exception {
        // proxied caster is not valid() -> the render frame short-circuits, so ticking is safe
        Terminable handle = Telegraphs.ring(3.0D).show(this.player);
        assertNotNull(handle);
        this.scheduler.tick(8); // would draw if the caster were valid
        handle.close();
        this.scheduler.tick(8); // stopped: nothing runs
    }

    @Test
    public void glowHighlightsResolvedTargetsAndRestoresOnClose() throws Exception {
        List<Boolean> glowCalls = new ArrayList<>();
        LivingEntity target = ProxyFakes.proxy(LivingEntity.class, Map.of(
                "getUniqueId", UUID.randomUUID(),
                "isGlowing", false,
                "setGlowing", (Function<Object[], Object>) args -> {
                    glowCalls.add((Boolean) args[0]);
                    return null;
                }));
        AbilityTargeting targeting = caster -> List.of(target);

        Terminable handle = Telegraphs.glow(targeting).show(this.player);
        this.scheduler.tick(); // entity-scheduler work applies the glow
        assertEquals(List.of(true), glowCalls);

        handle.close();
        this.scheduler.tick();
        assertEquals(List.of(true, false), glowCalls);
    }

    @Test
    public void glowSkipsAlreadyGlowingEntities() throws Exception {
        List<Boolean> glowCalls = new ArrayList<>();
        LivingEntity target = ProxyFakes.proxy(LivingEntity.class, Map.of(
                "getUniqueId", UUID.randomUUID(),
                "isGlowing", true,
                "setGlowing", (Function<Object[], Object>) args -> {
                    glowCalls.add((Boolean) args[0]);
                    return null;
                }));
        AbilityTargeting targeting = caster -> List.of(target);

        Terminable handle = Telegraphs.glow(targeting).show(this.player);
        this.scheduler.tick();
        handle.close();
        this.scheduler.tick();
        assertTrue(glowCalls.isEmpty(), "already-glowing entity must be left untouched");
    }

    @Test
    public void allShowsAndClosesEveryChild() throws Exception {
        AtomicInteger shows = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        Telegraph child = caster -> {
            shows.incrementAndGet();
            return closes::incrementAndGet;
        };

        Terminable handle = Telegraphs.all(child, child).show(this.player);
        assertEquals(2, shows.get());
        assertEquals(0, closes.get());

        handle.close();
        assertEquals(2, closes.get());
    }
}
