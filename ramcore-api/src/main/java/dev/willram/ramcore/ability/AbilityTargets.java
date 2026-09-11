package dev.willram.ramcore.ability;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.selector.Selectors;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Built-in {@link AbilityTargeting} strategies. Radius/nearest reuse the {@code selector} package;
 * {@link #lookingAt(double)} uses Paper's target ray-trace.
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

    private static double positive(double radius) {
        RamPreconditions.checkArgument(Double.isFinite(radius) && radius > 0.0D,
                "targeting radius must be finite and positive (was " + radius + ")",
                "pass a positive radius");
        return radius;
    }
}
