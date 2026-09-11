package dev.willram.ramcore.ability.telegraph;

import dev.willram.ramcore.ability.AbilityTargeting;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Built-in {@link Telegraph} previews. Particle shapes ({@link #ring}, {@link #cone}, {@link #line},
 * {@link #blockMarker}) redraw every {@link #DEFAULT_INTERVAL_TICKS} ticks on the caster's (or the
 * marker location's) scheduler and stop when their terminable is closed; a frame is skipped while the
 * caster is invalid/offline. {@link #glow} highlights the resolved targets server-side and restores
 * them on close. Compose several with {@link #all}.
 */
public final class Telegraphs {

    /** Default redraw interval for particle previews, in ticks. */
    public static final long DEFAULT_INTERVAL_TICKS = 4L;
    /** Default particle for previews. */
    public static final Particle DEFAULT_PARTICLE = Particle.END_ROD;

    private Telegraphs() {
    }

    /** A horizontal ring of {@code radius} at the caster's feet, using the default particle/interval. */
    @NotNull
    public static Telegraph ring(double radius) {
        return ring(radius, DEFAULT_PARTICLE, DEFAULT_INTERVAL_TICKS);
    }

    /**
     * A horizontal ring of {@code radius} at the caster's feet.
     *
     * @param radius        the ring radius in blocks
     * @param particle      the particle to draw with
     * @param intervalTicks redraw interval in ticks
     * @return the telegraph
     */
    @NotNull
    public static Telegraph ring(double radius, @NotNull Particle particle, long intervalTicks) {
        double r = positive(radius);
        requireNonNull(particle, "particle");
        long interval = interval(intervalTicks);
        int points = Math.max(8, (int) Math.ceil(r * 8.0D));
        return caster -> particleLoop(caster, interval, () -> {
            Location origin = caster.getLocation();
            World world = origin.getWorld();
            for (int i = 0; i < points; i++) {
                double a = (2.0D * Math.PI * i) / points;
                world.spawnParticle(particle, origin.getX() + (r * Math.cos(a)), origin.getY(),
                        origin.getZ() + (r * Math.sin(a)), 1);
            }
        });
    }

    /** A line from the caster's eye out to {@code range} along the look direction. */
    @NotNull
    public static Telegraph line(double range) {
        return line(range, DEFAULT_PARTICLE, DEFAULT_INTERVAL_TICKS);
    }

    /**
     * A line from the caster's eye out to {@code range} along the look direction.
     *
     * @param range         the line length in blocks
     * @param particle      the particle to draw with
     * @param intervalTicks redraw interval in ticks
     * @return the telegraph
     */
    @NotNull
    public static Telegraph line(double range, @NotNull Particle particle, long intervalTicks) {
        double r = positive(range);
        requireNonNull(particle, "particle");
        long interval = interval(intervalTicks);
        int steps = Math.max(4, (int) Math.ceil(r * 2.0D));
        return caster -> particleLoop(caster, interval, () -> {
            Location eye = caster.getEyeLocation();
            World world = eye.getWorld();
            Vector step = eye.getDirection().normalize().multiply(r / steps);
            Vector point = eye.toVector();
            for (int i = 0; i <= steps; i++) {
                world.spawnParticle(particle, point.getX(), point.getY(), point.getZ(), 1);
                point = point.add(step);
            }
        });
    }

    /** A ground sector of half-angle {@code angleDeg} out to {@code range} around the caster's aim. */
    @NotNull
    public static Telegraph cone(double range, double angleDeg) {
        return cone(range, angleDeg, DEFAULT_PARTICLE, DEFAULT_INTERVAL_TICKS);
    }

    /**
     * A sector (the two edge rays plus the far arc) of half-angle {@code angleDeg} out to
     * {@code range}, in the horizontal plane about the caster's facing.
     *
     * @param range         the sector range in blocks
     * @param angleDeg      the sector half-angle in degrees
     * @param particle      the particle to draw with
     * @param intervalTicks redraw interval in ticks
     * @return the telegraph
     */
    @NotNull
    public static Telegraph cone(double range, double angleDeg, @NotNull Particle particle, long intervalTicks) {
        double r = positive(range);
        double angle = angle(angleDeg);
        requireNonNull(particle, "particle");
        long interval = interval(intervalTicks);
        int steps = Math.max(4, (int) Math.ceil(r * 2.0D));
        int arc = Math.max(4, (int) Math.ceil((angle * 2.0D) / 5.0D));
        return caster -> particleLoop(caster, interval, () -> {
            Location eye = caster.getEyeLocation();
            World world = eye.getWorld();
            Vector look = flatten(eye.getDirection());
            // two edge rays
            for (double edge : new double[]{-angle, angle}) {
                Vector dir = rotateY(look, Math.toRadians(edge)).multiply(r / steps);
                Vector point = eye.toVector();
                for (int i = 0; i <= steps; i++) {
                    world.spawnParticle(particle, point.getX(), point.getY(), point.getZ(), 1);
                    point = point.add(dir);
                }
            }
            // far arc
            for (int i = 0; i <= arc; i++) {
                double a = -angle + ((2.0D * angle * i) / arc);
                Vector edge = rotateY(look, Math.toRadians(a)).multiply(r).add(eye.toVector());
                world.spawnParticle(particle, edge.getX(), edge.getY(), edge.getZ(), 1);
            }
        });
    }

    /** Outlines the top face of the block at {@code location} (a fixed ground marker). */
    @NotNull
    public static Telegraph blockMarker(@NotNull Location location) {
        return blockMarker(location, DEFAULT_PARTICLE, DEFAULT_INTERVAL_TICKS);
    }

    /**
     * Outlines the top face of the block at {@code location}. Rendered on the location's region
     * scheduler, so it is fixed in the world and independent of where the caster looks.
     *
     * @param location      the block location to mark (copied)
     * @param particle      the particle to draw with
     * @param intervalTicks redraw interval in ticks
     * @return the telegraph
     */
    @NotNull
    public static Telegraph blockMarker(@NotNull Location location, @NotNull Particle particle, long intervalTicks) {
        requireNonNull(location, "location");
        requireNonNull(particle, "particle");
        long interval = interval(intervalTicks);
        Location mark = location.clone();
        int steps = 8;
        return caster -> {
            Task task = Schedulers.runTimer(mark, 0L, interval, () -> {
                World world = mark.getWorld();
                if (world == null) {
                    return;
                }
                double bx = Math.floor(mark.getX());
                double by = Math.floor(mark.getY()) + 1.0D;
                double bz = Math.floor(mark.getZ());
                for (int i = 0; i <= steps; i++) {
                    double f = (double) i / steps;
                    world.spawnParticle(particle, bx + f, by, bz, 1);
                    world.spawnParticle(particle, bx + f, by, bz + 1.0D, 1);
                    world.spawnParticle(particle, bx, by, bz + f, 1);
                    world.spawnParticle(particle, bx + 1.0D, by, bz + f, 1);
                }
            });
            return task::stop;
        };
    }

    /**
     * Highlights the entities resolved by {@code targeting} at show time with a server-side glow,
     * restoring them when closed. Entities already glowing are left untouched. The only in-world
     * outline without a client mod.
     *
     * @param targeting the targeting whose resolved entities are highlighted
     * @return the telegraph
     */
    @NotNull
    public static Telegraph glow(@NotNull AbilityTargeting targeting) {
        requireNonNull(targeting, "targeting");
        return caster -> {
            List<LivingEntity> glowed = new ArrayList<>();
            for (LivingEntity target : targeting.resolve(caster)) {
                if (target.isGlowing()) {
                    continue; // leave already-glowing entities untouched
                }
                glowed.add(target);
                Schedulers.run(target, () -> target.setGlowing(true));
            }
            return () -> {
                for (LivingEntity target : glowed) {
                    Schedulers.run(target, () -> target.setGlowing(false));
                }
            };
        };
    }

    /** A telegraph that shows all of {@code telegraphs} together and clears them together. */
    @NotNull
    public static Telegraph all(@NotNull Telegraph... telegraphs) {
        requireNonNull(telegraphs, "telegraphs");
        Telegraph[] copy = telegraphs.clone();
        for (Telegraph telegraph : copy) {
            requireNonNull(telegraph, "telegraph");
        }
        return caster -> {
            CompositeTerminable composite = CompositeTerminable.create();
            for (Telegraph telegraph : copy) {
                composite.with(telegraph.show(caster));
            }
            return composite;
        };
    }

    private static Terminable particleLoop(@NotNull Player caster, long intervalTicks, @NotNull Runnable frame) {
        Task task = Schedulers.runTimer(caster, 0L, intervalTicks, () -> {
            if (!caster.isValid()) {
                return; // don't draw for an offline/dead caster; the terminable still stops it
            }
            frame.run();
        });
        return task::stop;
    }

    /** Drops the vertical component so cone/line math stays in the horizontal plane. */
    private static Vector flatten(@NotNull Vector direction) {
        Vector flat = new Vector(direction.getX(), 0.0D, direction.getZ());
        return flat.lengthSquared() <= 0.0D ? new Vector(1, 0, 0) : flat.normalize();
    }

    private static Vector rotateY(@NotNull Vector v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector((v.getX() * cos) + (v.getZ() * sin), v.getY(), (-v.getX() * sin) + (v.getZ() * cos));
    }

    private static double positive(double value) {
        RamPreconditions.checkArgument(Double.isFinite(value) && value > 0.0D,
                "telegraph size must be finite and positive (was " + value + ")", "pass a positive value");
        return value;
    }

    private static double angle(double angleDeg) {
        RamPreconditions.checkArgument(Double.isFinite(angleDeg) && angleDeg > 0.0D && angleDeg <= 180.0D,
                "telegraph cone angle must be finite and in (0, 180] (was " + angleDeg + ")",
                "pass a half-angle in degrees, e.g. 30");
        return angleDeg;
    }

    private static long interval(long intervalTicks) {
        RamPreconditions.checkArgument(intervalTicks > 0L, "telegraph interval must be positive",
                "pass intervalTicks > 0");
        return intervalTicks;
    }
}
