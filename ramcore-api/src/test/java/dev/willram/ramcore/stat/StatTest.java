package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StatTest {
    private static final ContentId HEALTH = ContentId.parse("test:health");

    @Test
    public void clampsToRange() {
        Stat stat = new Stat(HEALTH, 20.0D, 0.0D, 40.0D, StatFormat.INTEGER);
        assertEquals(0.0D, stat.clamp(-5.0D));
        assertEquals(40.0D, stat.clamp(100.0D));
        assertEquals(25.0D, stat.clamp(25.0D));
    }

    @Test
    public void rejectsMinGreaterThanMax() {
        assertThrows(RuntimeException.class,
                () -> new Stat(HEALTH, 5.0D, 10.0D, 0.0D, StatFormat.DECIMAL));
    }

    @Test
    public void rejectsBaseOutsideRange() {
        assertThrows(RuntimeException.class,
                () -> new Stat(HEALTH, 50.0D, 0.0D, 40.0D, StatFormat.DECIMAL));
    }

    @Test
    public void rejectsNonFinite() {
        assertThrows(RuntimeException.class,
                () -> new Stat(HEALTH, Double.NaN, 0.0D, 40.0D, StatFormat.DECIMAL));
    }

    @Test
    public void ofUsesFullRange() {
        Stat stat = Stat.of(HEALTH, 7.5D);
        assertEquals(7.5D, stat.base());
        assertEquals(StatFormat.DECIMAL, stat.format());
        assertEquals(1000.0D, stat.clamp(1000.0D));
    }

    @Test
    public void formats() {
        assertEquals("42", StatFormat.INTEGER.format(41.6D));
        assertEquals("42.5", StatFormat.DECIMAL.format(42.5D));
        assertEquals("25%", StatFormat.PERCENT.format(0.25D));
    }

    @Test
    public void toStringIsNamespaced() {
        assertTrue(HEALTH.toString().contains("test:health"));
    }
}
