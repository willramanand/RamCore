package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StatSnapshotTest {
    private static final ContentId HEALTH = ContentId.parse("test:health");
    private static final ContentId POWER = ContentId.parse("test:power");
    private static final ContentId UNKNOWN = ContentId.parse("test:unknown");

    private StatRegistry registry() {
        StatRegistry registry = new StatRegistry();
        registry.register("test", new Stat(HEALTH, 20.0D, 0.0D, 100.0D, StatFormat.INTEGER));
        registry.register("test", Stat.of(POWER, 5.0D));
        return registry;
    }

    @Test
    public void addThenMultiply() {
        StatSnapshot snapshot = StatSnapshot.compute(registry(), List.of(
                StatModifier.add(HEALTH, 10.0D, "a"),
                StatModifier.add(HEALTH, 5.0D, "b"),
                StatModifier.multiply(HEALTH, 0.10D, "c")));
        // (20 + 15) * 1.10 = 38.5
        assertEquals(38.5D, snapshot.value(HEALTH), 1.0e-9D);
    }

    @Test
    public void baseWhenNoModifiers() {
        StatSnapshot snapshot = StatSnapshot.compute(registry(), List.of());
        assertEquals(20.0D, snapshot.value(HEALTH));
        assertEquals(5.0D, snapshot.value(POWER));
    }

    @Test
    public void clampsToRange() {
        StatSnapshot snapshot = StatSnapshot.compute(registry(), List.of(
                StatModifier.add(HEALTH, 1000.0D, "a")));
        assertEquals(100.0D, snapshot.value(HEALTH));
    }

    @Test
    public void unknownStatModifierIgnored() {
        StatSnapshot snapshot = StatSnapshot.compute(registry(), List.of(
                StatModifier.add(UNKNOWN, 999.0D, "a")));
        assertTrue(snapshot.get(UNKNOWN).isEmpty());
        assertEquals(0.0D, snapshot.value(UNKNOWN));
        assertEquals(2, snapshot.values().size());
    }

    @Test
    public void emptySnapshotHasNoValues() {
        assertTrue(StatSnapshot.empty().values().isEmpty());
        assertEquals(0.0D, StatSnapshot.empty().value(HEALTH));
    }
}
