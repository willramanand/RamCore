package dev.willram.ramcore.region;

import dev.willram.ramcore.serialize.Position;
import dev.willram.ramcore.serialize.Region;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * Common lightweight region shapes.
 */
public final class RegionShapes {

    @NotNull
    public static RegionShape cuboid(@NotNull Region region) {
        requireNonNull(region, "region");
        return region::inRegion;
    }

    @NotNull
    public static RegionShape sphere(@NotNull Position center, double radius) {
        requireNonNull(center, "center");
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        double radiusSquared = radius * radius;
        return position -> position.getWorld().equals(center.getWorld())
                && squaredDistance(center, position) <= radiusSquared;
    }

    @NotNull
    public static RegionShape any(@NotNull Collection<? extends RegionShape> shapes) {
        requireNonNull(shapes, "shapes");
        return position -> shapes.stream().anyMatch(shape -> shape.contains(position));
    }

    @NotNull
    public static RegionShape all(@NotNull Collection<? extends RegionShape> shapes) {
        requireNonNull(shapes, "shapes");
        return position -> shapes.stream().allMatch(shape -> shape.contains(position));
    }

    private static double squaredDistance(Position a, Position b) {
        double x = a.getX() - b.getX();
        double y = a.getY() - b.getY();
        double z = a.getZ() - b.getZ();
        return x * x + y * y + z * z;
    }

    private RegionShapes() {
    }
}
