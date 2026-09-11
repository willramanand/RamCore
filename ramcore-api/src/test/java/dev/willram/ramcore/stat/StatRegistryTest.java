package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StatRegistryTest {
    private static final ContentId HEALTH = ContentId.parse("test:health");
    private static final ContentId POWER = ContentId.parse("test:power");

    @Test
    public void registerAndLookup() {
        StatRegistry registry = new StatRegistry();
        Stat health = Stat.of(HEALTH, 20.0D);
        registry.register("test", health);

        assertTrue(registry.contains(HEALTH));
        assertEquals(health, registry.get(HEALTH).orElseThrow());
        assertEquals(health, registry.require(HEALTH));
        assertTrue(registry.ids().contains(HEALTH));
    }

    @Test
    public void requireMissingThrows() {
        StatRegistry registry = new StatRegistry();
        assertThrows(RuntimeException.class, () -> registry.require(POWER));
        assertTrue(registry.get(POWER).isEmpty());
    }

    @Test
    public void duplicateRejected() {
        StatRegistry registry = new StatRegistry();
        registry.register("test", Stat.of(HEALTH, 20.0D));
        assertThrows(RuntimeException.class, () -> registry.register("test", Stat.of(HEALTH, 30.0D)));
    }

    @Test
    public void unregisterOwnerClearsOnlyThatOwner() {
        StatRegistry registry = new StatRegistry();
        registry.register("a", Stat.of(HEALTH, 20.0D));
        registry.register("b", Stat.of(POWER, 5.0D));

        assertEquals(1, registry.unregisterOwner("a"));
        assertFalse(registry.contains(HEALTH));
        assertTrue(registry.contains(POWER));
    }

    @Test
    public void modifierFactories() {
        StatModifier add = StatModifier.add(HEALTH, 5.0D, "item:helmet");
        StatModifier mul = StatModifier.multiply(HEALTH, 0.10D, "buff:rage");
        assertEquals(StatOperation.ADD, add.operation());
        assertEquals(StatOperation.MULTIPLY, mul.operation());
        assertEquals(0.10D, mul.amount());
        assertThrows(RuntimeException.class, () -> StatModifier.add(HEALTH, 1.0D, " "));
        assertThrows(RuntimeException.class, () -> StatModifier.add(HEALTH, Double.POSITIVE_INFINITY, "x"));
    }
}
