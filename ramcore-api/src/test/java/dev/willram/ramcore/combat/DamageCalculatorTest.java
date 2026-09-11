package dev.willram.ramcore.combat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.stat.Stat;
import dev.willram.ramcore.stat.StatModifier;
import dev.willram.ramcore.stat.StatRegistry;
import dev.willram.ramcore.stat.StatSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DamageCalculatorTest {

    private static StatRegistry combatRegistry() {
        StatRegistry registry = new StatRegistry();
        for (ContentId id : List.of(CombatStats.CRIT_CHANCE, CombatStats.CRIT_DAMAGE, CombatStats.LIFESTEAL,
                CombatStats.DEFENSE, CombatStats.resistance("fire"))) {
            registry.register("test", new Stat(id, 0.0D, 0.0D, 100000.0D, dev.willram.ramcore.stat.StatFormat.DECIMAL));
        }
        return registry;
    }

    private static StatSnapshot of(StatRegistry registry, Object... idValuePairs) {
        List<StatModifier> mods = new ArrayList<>();
        for (int i = 0; i < idValuePairs.length; i += 2) {
            mods.add(StatModifier.add((ContentId) idValuePairs[i], (double) idValuePairs[i + 1], "test"));
        }
        return StatSnapshot.compute(registry, mods);
    }

    private static DamageCalculator alwaysCrit() {
        return new DamageCalculator(() -> 0.0D);
    }

    private static DamageCalculator neverCrit() {
        return new DamageCalculator(() -> 0.999999D);
    }

    @Test
    public void passthroughWithNoStats() {
        StatRegistry r = combatRegistry();
        DamageBreakdown b = neverCrit().calculate(10.0D, of(r), of(r));
        assertEquals(10.0D, b.finalDamage());
        assertFalse(b.crit());
        assertEquals(0.0D, b.lifestealHealed());
    }

    @Test
    public void critMultipliesDamage() {
        StatRegistry r = combatRegistry();
        StatSnapshot attacker = of(r, CombatStats.CRIT_CHANCE, 1.0D, CombatStats.CRIT_DAMAGE, 0.5D);
        DamageBreakdown b = alwaysCrit().calculate(10.0D, attacker, of(r));
        assertTrue(b.crit());
        assertEquals(15.0D, b.afterCrit());
        assertEquals(15.0D, b.finalDamage());
    }

    @Test
    public void critChanceZeroNeverCrits() {
        StatRegistry r = combatRegistry();
        DamageBreakdown b = alwaysCrit().calculate(10.0D, of(r), of(r));
        assertFalse(b.crit());
        assertEquals(10.0D, b.finalDamage());
    }

    @Test
    public void resistanceReducesTypedDamage() {
        StatRegistry r = combatRegistry();
        StatSnapshot defender = of(r, CombatStats.resistance("fire"), 0.5D);
        DamageBreakdown b = neverCrit().calculate(10.0D, of(r), defender, "fire");
        assertEquals(5.0D, b.resistedAmount());
        assertEquals(5.0D, b.finalDamage());
    }

    @Test
    public void resistanceClampedAt95Percent() {
        StatRegistry r = combatRegistry();
        StatSnapshot defender = of(r, CombatStats.resistance("fire"), 5.0D);
        DamageBreakdown b = neverCrit().calculate(100.0D, of(r), defender, "fire");
        assertEquals(5.0D, b.finalDamage(), 1.0e-9D); // 100 * (1 - 0.95)
    }

    @Test
    public void defenseSubtractsAndFloorsAtZero() {
        StatRegistry r = combatRegistry();
        StatSnapshot defender = of(r, CombatStats.DEFENSE, 4.0D);
        DamageBreakdown b = neverCrit().calculate(10.0D, of(r), defender);
        assertEquals(4.0D, b.mitigatedAmount());
        assertEquals(6.0D, b.finalDamage());

        StatSnapshot heavy = of(r, CombatStats.DEFENSE, 999.0D);
        assertEquals(0.0D, neverCrit().calculate(10.0D, of(r), heavy).finalDamage());
    }

    @Test
    public void lifestealHealsFractionOfFinal() {
        StatRegistry r = combatRegistry();
        StatSnapshot attacker = of(r, CombatStats.LIFESTEAL, 0.2D);
        DamageBreakdown b = neverCrit().calculate(50.0D, attacker, of(r));
        assertEquals(10.0D, b.lifestealHealed());
    }

    @Test
    public void damageProfileWithStatsSetsAmountAndBreakdown() {
        StatRegistry r = combatRegistry();
        StatSnapshot attacker = of(r, CombatStats.CRIT_CHANCE, 1.0D, CombatStats.CRIT_DAMAGE, 1.0D,
                CombatStats.LIFESTEAL, 0.5D);
        StatSnapshot defender = of(r, CombatStats.DEFENSE, 2.0D);

        DamageProfile profile = DamageProfile.amount(10.0D)
                .calculator(alwaysCrit())
                .withStats(attacker, defender)
                .build();

        // 10 crit x2 = 20, minus 2 defense = 18
        assertEquals(18.0D, profile.amount());
        DamageBreakdown b = profile.breakdown().orElseThrow();
        assertTrue(b.crit());
        assertEquals(9.0D, b.lifestealHealed()); // 18 * 0.5
    }

    @Test
    public void damageProfileWithoutStatsHasNoBreakdown() {
        DamageProfile profile = DamageProfile.amount(7.0D).build();
        assertTrue(profile.breakdown().isEmpty());
        assertEquals(7.0D, profile.amount());
    }
}
