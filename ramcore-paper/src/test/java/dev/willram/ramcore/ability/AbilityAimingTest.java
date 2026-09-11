package dev.willram.ramcore.ability;

import dev.willram.ramcore.ability.AbilityAiming.AimOptions;
import dev.willram.ramcore.ability.AbilityAiming.Aiming;
import dev.willram.ramcore.ability.telegraph.Telegraph;
import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilityAimingTest {

    private FakeScheduler scheduler;
    private Player player;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.player = ProxyFakes.proxy(Player.class, Map.of(
                "getUniqueId", UUID.randomUUID(),
                "isValid", true));
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    private static Telegraph noopTelegraph(AtomicInteger closes) {
        return caster -> closes::incrementAndGet;
    }

    private Aiming aim(AbilityTargeting targeting, Telegraph telegraph, AimOptions options) {
        return AbilityAiming.start(this.player, targeting, telegraph, options);
    }

    @Test
    public void optionsRejectNonPositiveDurations() {
        assertThrows(RuntimeException.class, () -> new AimOptions(0L, 200L, true, true));
        assertThrows(RuntimeException.class, () -> new AimOptions(2L, 0L, true, true));
    }

    @Test
    public void confirmResolvesTargetsAndCompletesResult() throws Exception {
        LivingEntity target = ProxyFakes.proxy(LivingEntity.class, Map.of("getUniqueId", UUID.randomUUID()));
        AtomicInteger closes = new AtomicInteger();
        Aiming aiming = aim(caster -> List.of(target), noopTelegraph(closes),
                AimOptions.defaults().withGlowTargets(false));

        assertTrue(aiming.isActive());
        aiming.confirm();

        assertFalse(aiming.isActive());
        assertTrue(aiming.result().isDone());
        assertEquals(List.of(target), aiming.result().get());
        assertEquals(1, closes.get(), "telegraph cleared on confirm");
    }

    @Test
    public void cancelCompletesResultExceptionally() {
        AtomicInteger closes = new AtomicInteger();
        Aiming aiming = aim(caster -> List.of(), noopTelegraph(closes),
                AimOptions.defaults().withGlowTargets(false));

        aiming.cancel();

        assertFalse(aiming.isActive());
        assertTrue(aiming.result().isDone());
        assertThrows(CancellationException.class, () -> aiming.result().get());
        assertEquals(1, closes.get(), "telegraph cleared on cancel");
    }

    @Test
    public void timeoutCancelsTheAim() {
        AtomicInteger closes = new AtomicInteger();
        Aiming aiming = aim(caster -> List.of(), noopTelegraph(closes),
                AimOptions.defaults().withGlowTargets(false).withTimeoutTicks(10L));

        this.scheduler.tick(10);

        assertFalse(aiming.isActive());
        assertThrows(CancellationException.class, () -> aiming.result().get());
    }

    @Test
    public void invalidPlayerCancelsOnRefresh() {
        Player gone = ProxyFakes.proxy(Player.class, Map.of(
                "getUniqueId", UUID.randomUUID(),
                "isValid", false));
        AtomicInteger closes = new AtomicInteger();
        Aiming aiming = AbilityAiming.start(gone, caster -> List.of(), noopTelegraph(closes),
                AimOptions.defaults().withGlowTargets(false));

        this.scheduler.tick(2); // first refresh sees an invalid player
        assertFalse(aiming.isActive());
    }

    @Test
    public void glowFollowsTargetsAndClearsOnClose() {
        List<Boolean> glowCalls = new ArrayList<>();
        LivingEntity target = ProxyFakes.proxy(LivingEntity.class, Map.of(
                "getUniqueId", UUID.randomUUID(),
                "isGlowing", false,
                "setGlowing", (Function<Object[], Object>) args -> {
                    glowCalls.add((Boolean) args[0]);
                    return null;
                }));
        Aiming aiming = aim(caster -> List.of(target), noopTelegraph(new AtomicInteger()),
                AimOptions.defaults());

        this.scheduler.tick(2); // refresh -> glow scheduled -> entity task runs
        assertEquals(List.of(true), glowCalls);

        aiming.confirm();
        this.scheduler.tick(); // entity task for un-glow runs
        assertEquals(List.of(true, false), glowCalls);
    }
}
