package dev.willram.ramcore.ability;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.selector.Selectors;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Built-in {@link AbilityTargeting} strategies. Radius/nearest reuse the {@code selector} package;
 * {@link #lookingAt(double)} uses Paper's strict target ray-trace, while {@link #rayTrace} adds
 * aim-assist. {@link #cone}, {@link #beam}, and {@link #groundRadius} use the pure
 * {@link AbilityGeometry} shapes to pick living entities around the caster's look direction.
 */
public final class AbilityTargets {

    private AbilityTargets() {
    }

    /** Targets the caster only. */
    @NotNull
    public static AbilityTargeting self() {
        return caster -> List.of(caster);
    }

    /** No targets (self-contained effects/actions). */
    @NotNull
    public static AbilityTargeting none() {
        return caster -> List.of();
    }

    /**
     * Every living entity within {@code radius} blocks of the caster, excluding the caster.
     *
     * @param radius the radius in blocks
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting radius(double radius) {
        double r = positive(radius);
        return caster -> {
            Location origin = caster.getLocation();
            return Selectors.entity(LivingEntity.class)
                    .within(origin, r)
                    .filter(entity -> !entity.getUniqueId().equals(caster.getUniqueId()))
                    .select(caster.getWorld().getNearbyEntities(origin, r, r, r));
        };
    }

    /**
     * The single nearest living entity within {@code radius}, excluding the caster.
     *
     * @param radius the radius in blocks
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting nearest(double radius) {
        double r = positive(radius);
        return caster -> {
            Location origin = caster.getLocation();
            return Selectors.entity(LivingEntity.class)
                    .within(origin, r)
                    .filter(entity -> !entity.getUniqueId().equals(caster.getUniqueId()))
                    .nearest(origin)
                    .limit(1)
                    .select(caster.getWorld().getNearbyEntities(origin, r, r, r));
        };
    }

    /**
     * The living entity the caster is looking at within {@code range}, or empty.
     *
     * @param range the max range in blocks
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting lookingAt(double range) {
        double r = positive(range);
        return caster -> {
            Entity target = caster.getTargetEntity((int) Math.ceil(r));
            return target instanceof LivingEntity living ? List.of(living) : List.of();
        };
    }

    /**
     * The single living entity under the caster's aim within {@code range}, using a fat ray of the
     * given {@code raySize} for aim assist. A more forgiving upgrade of {@link #lookingAt(double)}:
     * a larger {@code raySize} makes the aim easier to land. Returns empty if the ray hits nothing
     * living first.
     *
     * @param range   the max range in blocks
     * @param raySize the aim-assist radius of the ray in blocks (e.g. {@code 0.3})
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting rayTrace(double range, double raySize) {
        double r = positive(range);
        double size = positive(raySize);
        return caster -> {
            Location eye = caster.getEyeLocation();
            RayTraceResult result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), r, size,
                    entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(caster.getUniqueId()));
            return result != null && result.getHitEntity() instanceof LivingEntity living
                    ? List.of(living) : List.of();
        };
    }

    /**
     * Every living entity within {@code range} that also lies within {@code angleDeg} of the
     * caster's look direction (a cone of half-angle {@code angleDeg} about the aim axis), excluding
     * the caster.
     *
     * @param range    the max range in blocks
     * @param angleDeg the cone half-angle in degrees
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting cone(double range, double angleDeg) {
        double r = positive(range);
        double angle = angle(angleDeg);
        return caster -> coneCandidates(caster, r, angle);
    }

    /**
     * The single nearest living entity within the cone described by {@link #cone(double, double)},
     * or empty. Useful for a forgiving single-target lock-on.
     *
     * @param range    the max range in blocks
     * @param angleDeg the cone half-angle in degrees
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting nearestInCone(double range, double angleDeg) {
        double r = positive(range);
        double angle = angle(angleDeg);
        return caster -> {
            Location eye = caster.getEyeLocation();
            return coneCandidates(caster, r, angle).stream()
                    .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(eye)))
                    .map(List::of)
                    .orElseGet(List::of);
        };
    }

    /**
     * Every living entity within {@code width} blocks of the caster's look ray, up to {@code range}
     * blocks ahead (a line/beam AoE), excluding the caster.
     *
     * @param range the max range in blocks
     * @param width the half-width of the beam in blocks
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting beam(double range, double width) {
        double r = positive(range);
        double w = positive(width);
        return caster -> {
            Location eye = caster.getEyeLocation();
            Vector dir = eye.getDirection();
            List<LivingEntity> hits = new ArrayList<>();
            for (LivingEntity living : nearbyLiving(caster, r)) {
                Vector toTarget = center(living).subtract(eye.toVector());
                if (AbilityGeometry.withinBeam(toTarget, dir, r, w)) {
                    hits.add(living);
                }
            }
            return List.copyOf(hits);
        };
    }

    /**
     * A ground-targeted AoE: ray-traces to the block the caster is aiming at within {@code range},
     * then selects every living entity within {@code radius} of that point (the caster included only
     * if in range). Empty if the caster is not aiming at a block.
     *
     * @param range  the max aim range in blocks
     * @param radius the AoE radius around the target block in blocks
     * @return the targeting
     */
    @NotNull
    public static AbilityTargeting groundRadius(double range, double radius) {
        double r = positive(range);
        double rad = positive(radius);
        return caster -> {
            Block block = caster.getTargetBlockExact((int) Math.ceil(r));
            if (block == null) {
                return List.of();
            }
            Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
            List<LivingEntity> hits = new ArrayList<>();
            for (Entity entity : caster.getWorld().getNearbyEntities(center, rad, rad, rad)) {
                if (entity instanceof LivingEntity living && living.getLocation().distanceSquared(center) <= rad * rad) {
                    hits.add(living);
                }
            }
            return List.copyOf(hits);
        };
    }

    private static List<LivingEntity> coneCandidates(@NotNull Player caster, double range,
            double angleDeg) {
        Location eye = caster.getEyeLocation();
        Vector look = eye.getDirection();
        List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity living : nearbyLiving(caster, range)) {
            Vector toTarget = center(living).subtract(eye.toVector());
            if (toTarget.lengthSquared() <= range * range && AbilityGeometry.withinCone(look, toTarget, angleDeg)) {
                hits.add(living);
            }
        }
        return List.copyOf(hits);
    }

    private static List<LivingEntity> nearbyLiving(@NotNull Player caster, double range) {
        Location origin = caster.getLocation();
        List<LivingEntity> living = new ArrayList<>();
        for (Entity entity : caster.getWorld().getNearbyEntities(origin, range, range, range)) {
            if (entity instanceof LivingEntity le && !entity.getUniqueId().equals(caster.getUniqueId())) {
                living.add(le);
            }
        }
        return living;
    }

    private static Vector center(@NotNull LivingEntity entity) {
        return entity.getBoundingBox().getCenter();
    }

    private static double angle(double angleDeg) {
        RamPreconditions.checkArgument(Double.isFinite(angleDeg) && angleDeg > 0.0D && angleDeg <= 180.0D,
                "cone angle must be finite and in (0, 180] (was " + angleDeg + ")",
                "pass a cone half-angle in degrees, e.g. 30");
        return angleDeg;
    }

    private static double positive(double radius) {
        RamPreconditions.checkArgument(Double.isFinite(radius) && radius > 0.0D,
                "targeting radius must be finite and positive (was " + radius + ")",
                "pass a positive radius");
        return radius;
    }
}
