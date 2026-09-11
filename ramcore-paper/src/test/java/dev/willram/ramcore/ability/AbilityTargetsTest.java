package dev.willram.ramcore.ability;

import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilityTargetsTest {

    private static Player player() {
        return ProxyFakes.proxy(Player.class, Map.of("getUniqueId", UUID.randomUUID()));
    }

    @Test
    public void selfTargetsCaster() {
        Player caster = player();
        List<LivingEntity> targets = AbilityTargets.self().resolve(caster);
        assertEquals(1, targets.size());
        assertSame(caster, targets.get(0));
    }

    @Test
    public void noneIsEmpty() {
        assertTrue(AbilityTargets.none().resolve(player()).isEmpty());
    }

    @Test
    public void lookingAtNothingIsEmpty() {
        // proxied player returns null for getTargetEntity -> no target
        assertTrue(AbilityTargets.lookingAt(10.0D).resolve(player()).isEmpty());
    }

    @Test
    public void rejectsNonPositiveRadius() {
        assertThrows(RuntimeException.class, () -> AbilityTargets.radius(0.0D));
        assertThrows(RuntimeException.class, () -> AbilityTargets.nearest(-1.0D));
    }
}
