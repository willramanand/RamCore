package dev.willram.ramcore.path;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test/offline path backend with deterministic state.
 */
public final class InMemoryPathBackend implements PathBackend {
    private final Mob mob;
    private Location currentLocation;
    private Pathfinder.PathResult nextPath;
    private Pathfinder.PathResult currentPath;
    private boolean hasPath;
    private boolean moveAccepted = true;
    private int moveAttempts;
    private PathNavigationProfile lastProfile = PathNavigationProfile.unchanged();

    public InMemoryPathBackend(@NotNull Mob mob, @NotNull Location currentLocation) {
        this.mob = Objects.requireNonNull(mob, "mob");
        this.currentLocation = Objects.requireNonNull(currentLocation, "currentLocation").clone();
    }

    @NotNull
    @Override
    public Mob mob() {
        return this.mob;
    }

    @NotNull
    @Override
    public Location currentLocation() {
        return this.currentLocation.clone();
    }

    public InMemoryPathBackend currentLocation(@NotNull Location currentLocation) {
        this.currentLocation = Objects.requireNonNull(currentLocation, "currentLocation").clone();
        return this;
    }

    public InMemoryPathBackend path(@Nullable Pathfinder.PathResult path) {
        this.nextPath = path;
        return this;
    }

    public InMemoryPathBackend path(@NotNull Location... points) {
        return path(SimplePathResult.of(List.of(points)));
    }

    public InMemoryPathBackend moveAccepted(boolean moveAccepted) {
        this.moveAccepted = moveAccepted;
        return this;
    }

    public int moveAttempts() {
        return this.moveAttempts;
    }

    @NotNull
    public PathNavigationProfile lastProfile() {
        return this.lastProfile;
    }

    @Nullable
    @Override
    public Pathfinder.PathResult findPath(@NotNull Location location) {
        if (this.nextPath != null) {
            return this.nextPath;
        }
        List<Location> points = new ArrayList<>();
        points.add(this.currentLocation);
        points.add(Objects.requireNonNull(location, "location"));
        return SimplePathResult.of(points);
    }

    @Nullable
    @Override
    public Pathfinder.PathResult findPath(@NotNull Entity entity) {
        return findPath(Objects.requireNonNull(entity, "entity").getLocation());
    }

    @Override
    public boolean moveTo(@NotNull Pathfinder.PathResult path, double speed) {
        this.moveAttempts++;
        if (!this.moveAccepted) {
            return false;
        }
        this.currentPath = Objects.requireNonNull(path, "path");
        this.hasPath = true;
        return true;
    }

    @Override
    public boolean hasPath() {
        return this.hasPath;
    }

    @Nullable
    @Override
    public Pathfinder.PathResult currentPath() {
        return this.currentPath;
    }

    @Override
    public void stop() {
        this.hasPath = false;
        this.currentPath = null;
    }

    @Override
    public void applyNavigationProfile(@NotNull PathNavigationProfile profile) {
        this.lastProfile = Objects.requireNonNull(profile, "profile");
    }
}
