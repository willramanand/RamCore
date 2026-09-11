package dev.willram.ramcore.ability;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Pure vector geometry shared by the {@link AbilityTargets} aim strategies (cone, beam) and the
 * telegraph shapes. Every method is side-effect free and server-free: it operates on
 * {@link Vector} inputs so it can be unit-tested without a world. Entity queries themselves live in
 * {@link AbilityTargets}; this class only decides whether a point lies inside a shape.
 */
final class AbilityGeometry {

    private AbilityGeometry() {
    }

    /**
     * The angle in degrees between two vectors, in {@code [0, 180]}. Returns {@code 180} if either
     * vector has zero length (treated as pointing away).
     */
    static double angleBetweenDeg(@NotNull Vector a, @NotNull Vector b) {
        double lenSq = a.lengthSquared() * b.lengthSquared();
        if (lenSq <= 0.0D) {
            return 180.0D;
        }
        double cos = a.dot(b) / Math.sqrt(lenSq);
        // guard floating-point drift outside [-1, 1] before acos
        cos = Math.max(-1.0D, Math.min(1.0D, cos));
        return Math.toDegrees(Math.acos(cos));
    }

    /**
     * Whether {@code toTarget} (origin&rarr;target) falls within {@code maxAngleDeg} of the
     * {@code look} direction &mdash; i.e. inside a cone of half-angle {@code maxAngleDeg} about the
     * look axis. A zero-length {@code toTarget} (target at the origin) counts as inside.
     */
    static boolean withinCone(@NotNull Vector look, @NotNull Vector toTarget, double maxAngleDeg) {
        if (toTarget.lengthSquared() <= 0.0D) {
            return true;
        }
        return angleBetweenDeg(look, toTarget) <= maxAngleDeg;
    }

    /**
     * The signed projection of {@code toTarget} onto {@code unitDir} &mdash; the distance along the
     * ray at which the target is closest. Assumes {@code unitDir} is normalised. Negative means the
     * target is behind the origin.
     */
    static double projection(@NotNull Vector toTarget, @NotNull Vector unitDir) {
        return toTarget.dot(unitDir);
    }

    /**
     * The squared perpendicular distance from the target to the infinite line through the origin
     * along {@code unitDir}. Assumes {@code unitDir} is normalised.
     */
    static double perpendicularDistanceSquared(@NotNull Vector toTarget, @NotNull Vector unitDir) {
        double t = projection(toTarget, unitDir);
        double perpSq = toTarget.lengthSquared() - (t * t);
        return Math.max(0.0D, perpSq);
    }

    /**
     * Whether {@code toTarget} lies within a beam of half-width {@code width} extending {@code range}
     * blocks from the origin along {@code unitDir}: the target must be in front of the origin, no
     * further than {@code range} along the axis, and no more than {@code width} off it. Assumes
     * {@code unitDir} is normalised.
     */
    static boolean withinBeam(@NotNull Vector toTarget, @NotNull Vector unitDir, double range, double width) {
        double t = projection(toTarget, unitDir);
        if (t < 0.0D || t > range) {
            return false;
        }
        return perpendicularDistanceSquared(toTarget, unitDir) <= (width * width);
    }
}
