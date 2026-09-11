package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.stat.Stat;
import dev.willram.ramcore.stat.StatModifier;
import dev.willram.ramcore.stat.StatRegistry;
import dev.willram.ramcore.stat.StatSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbilityModelTest {
    private static final ContentId MANA = ContentId.parse("test:mana");
    private static final ContentId FIREBALL = ContentId.parse("test:fireball");
    private static final ContentId BLINK = ContentId.parse("test:blink");

    @Test
    public void statCostAffordability() {
        StatRegistry registry = new StatRegistry();
        registry.register("test", Stat.of(MANA, 0.0D));
        StatSnapshot low = StatSnapshot.compute(registry, List.of(StatModifier.add(MANA, 10.0D, "s")));
        StatSnapshot high = StatSnapshot.compute(registry, List.of(StatModifier.add(MANA, 50.0D, "s")));

        StatCost cost = new StatCost(MANA, 30.0D);
        assertFalse(cost.affordable(low));
        assertTrue(cost.affordable(high));
    }

    @Test
    public void statCostRejectsBadAmount() {
        assertThrows(RuntimeException.class, () -> new StatCost(MANA, -1.0D));
        assertThrows(RuntimeException.class, () -> new StatCost(MANA, Double.NaN));
    }

    @Test
    public void abilityBuilderDefaults() {
        Ability ability = Ability.builder(FIREBALL).build();
        assertEquals(FIREBALL, ability.id());
        assertEquals(Duration.ZERO, ability.cooldown());
        assertEquals(0L, ability.castTicks());
        assertTrue(ability.cost().isEmpty());
        assertTrue(ability.effects().isEmpty());
        assertEquals(AbilityAction.NONE, ability.action());
    }

    @Test
    public void abilityBuilderValues() {
        Ability ability = Ability.builder(FIREBALL)
                .cooldown(Duration.ofSeconds(5))
                .castTicks(40)
                .cost(MANA, 20.0D)
                .build();
        assertEquals(Duration.ofSeconds(5), ability.cooldown());
        assertEquals(40L, ability.castTicks());
        assertEquals(20.0D, ability.cost().orElseThrow().amount());
        assertEquals(MANA, ability.cost().orElseThrow().statId());
    }

    @Test
    public void abilityBuilderRejectsNegatives() {
        assertThrows(RuntimeException.class, () -> Ability.builder(FIREBALL).cooldown(Duration.ofSeconds(-1)));
        assertThrows(RuntimeException.class, () -> Ability.builder(FIREBALL).castTicks(-1));
    }

    @Test
    public void registryLifecycle() {
        AbilityRegistry registry = new AbilityRegistry();
        Ability fireball = Ability.builder(FIREBALL).build();
        registry.register("test", fireball);

        assertTrue(registry.contains(FIREBALL));
        assertEquals(fireball, registry.require(FIREBALL));
        assertTrue(registry.ids().contains(FIREBALL));
        assertThrows(RuntimeException.class, () -> registry.require(BLINK));
        assertThrows(RuntimeException.class, () -> registry.register("test", fireball));

        registry.register("other", Ability.builder(BLINK).build());
        assertEquals(1, registry.unregisterOwner("test"));
        assertFalse(registry.contains(FIREBALL));
        assertTrue(registry.contains(BLINK));
    }
}
