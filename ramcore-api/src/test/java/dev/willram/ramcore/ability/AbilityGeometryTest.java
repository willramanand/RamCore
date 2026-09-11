package dev.willram.ramcore.ability;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure geometry behind the cone and beam targeters &mdash; no world required. */
public final class AbilityGeometryTest {

    private static final double EPS = 1.0e-9D;

    @Test
    public void angleBetweenIsZeroForSameDirection() {
        assertEquals(0.0D, AbilityGeometry.angleBetweenDeg(new Vector(1, 0, 0), new Vector(5, 0, 0)), EPS);
    }

    @Test
    public void angleBetweenIsNinetyForPerpendicular() {
        assertEquals(90.0D, AbilityGeometry.angleBetweenDeg(new Vector(1, 0, 0), new Vector(0, 0, 1)), EPS);
    }

    @Test
    public void angleBetweenIsOneEightyForOpposite() {
        assertEquals(180.0D, AbilityGeometry.angleBetweenDeg(new Vector(1, 0, 0), new Vector(-1, 0, 0)), EPS);
    }

    @Test
    public void zeroLengthVectorCountsAsFacingAway() {
        assertEquals(180.0D, AbilityGeometry.angleBetweenDeg(new Vector(1, 0, 0), new Vector(0, 0, 0)), EPS);
    }

    @Test
    public void coneIncludesTargetInsideAngle() {
        Vector look = new Vector(1, 0, 0);
        // 30 degrees off the look axis, inside a 45-degree cone
        Vector toTarget = new Vector(Math.cos(Math.toRadians(30)), 0, Math.sin(Math.toRadians(30)));
        assertTrue(AbilityGeometry.withinCone(look, toTarget, 45.0D));
    }

    @Test
    public void coneExcludesTargetOutsideAngle() {
        Vector look = new Vector(1, 0, 0);
        Vector toTarget = new Vector(Math.cos(Math.toRadians(60)), 0, Math.sin(Math.toRadians(60)));
        assertFalse(AbilityGeometry.withinCone(look, toTarget, 45.0D));
    }

    @Test
    public void coneExcludesTargetBehindCaster() {
        assertFalse(AbilityGeometry.withinCone(new Vector(1, 0, 0), new Vector(-1, 0, 0), 45.0D));
    }

    @Test
    public void coneIncludesTargetAtOrigin() {
        assertTrue(AbilityGeometry.withinCone(new Vector(1, 0, 0), new Vector(0, 0, 0), 10.0D));
    }

    @Test
    public void projectionIsDistanceAlongAxis() {
        assertEquals(5.0D, AbilityGeometry.projection(new Vector(5, 2, 0), new Vector(1, 0, 0)), EPS);
    }

    @Test
    public void perpendicularDistanceIgnoresAxisComponent() {
        // target 5 ahead, 2 to the side -> perpendicular distance is 2
        double perpSq = AbilityGeometry.perpendicularDistanceSquared(new Vector(5, 0, 2), new Vector(1, 0, 0));
        assertEquals(4.0D, perpSq, EPS);
    }

    @Test
    public void beamIncludesTargetWithinWidthAndRange() {
        // 5 blocks ahead, 0.5 off axis, in a 10-block beam of half-width 1
        assertTrue(AbilityGeometry.withinBeam(new Vector(5, 0, 0.5), new Vector(1, 0, 0), 10.0D, 1.0D));
    }

    @Test
    public void beamExcludesTargetTooWide() {
        assertFalse(AbilityGeometry.withinBeam(new Vector(5, 0, 2), new Vector(1, 0, 0), 10.0D, 1.0D));
    }

    @Test
    public void beamExcludesTargetBeyondRange() {
        assertFalse(AbilityGeometry.withinBeam(new Vector(15, 0, 0), new Vector(1, 0, 0), 10.0D, 1.0D));
    }

    @Test
    public void beamExcludesTargetBehindCaster() {
        assertFalse(AbilityGeometry.withinBeam(new Vector(-3, 0, 0), new Vector(1, 0, 0), 10.0D, 1.0D));
    }
}
