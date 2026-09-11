package dev.willram.ramcore.path;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Simple immutable {@link Pathfinder.PathResult} implementation for tests and
 * in-memory planning.
 */
public final class SimplePathResult implements Pathfinder.PathResult {
    private final List<Location> points;
    private final int nextPointIndex;
    private final boolean canReachFinalPoint;

    public SimplePathResult(@NotNull List<Location> points, int nextPointIndex, boolean canReachFinalPoint) {
        this.points = Objects.requireNonNull(points, "points").stream().map(Location::clone).toList();
        this.nextPointIndex = Math.max(0, nextPointIndex);
        this.canReachFinalPoint = canReachFinalPoint;
    }

    public static SimplePathResult of(@NotNull List<Location> points) {
        return new SimplePathResult(points, 0, true);
    }

    @NotNull
    @Override
    public List<Location> getPoints() {
        return this.points.stream().map(Location::clone).toList();
    }

    @Override
    public int getNextPointIndex() {
        return this.nextPointIndex;
    }

    @Nullable
    @Override
    public Location getNextPoint() {
        if (this.nextPointIndex < 0 || this.nextPointIndex >= this.points.size()) {
            return null;
        }
        return this.points.get(this.nextPointIndex).clone();
    }

    @Nullable
    @Override
    public Location getFinalPoint() {
        if (this.points.isEmpty()) {
            return null;
        }
        return this.points.get(this.points.size() - 1).clone();
    }

    @Override
    public boolean canReachFinalPoint() {
        return this.canReachFinalPoint;
    }
}
