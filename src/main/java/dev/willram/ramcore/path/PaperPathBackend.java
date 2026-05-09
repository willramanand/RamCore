package dev.willram.ramcore.path;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Path backend backed by Paper's public {@link Pathfinder} API.
 */
public final class PaperPathBackend implements PathBackend {
    private final Mob mob;

    public PaperPathBackend(@NotNull Mob mob) {
        this.mob = Objects.requireNonNull(mob, "mob");
    }

    @NotNull
    @Override
    public Mob mob() {
        return this.mob;
    }

    @Nullable
    @Override
    public Pathfinder.PathResult findPath(@NotNull Location location) {
        return this.mob.getPathfinder().findPath(Objects.requireNonNull(location, "location"));
    }

    @Nullable
    @Override
    public Pathfinder.PathResult findPath(@NotNull Entity entity) {
        return this.mob.getPathfinder().findPath(Objects.requireNonNull(entity, "entity"));
    }

    @Override
    public boolean moveTo(@NotNull Pathfinder.PathResult path, double speed) {
        return this.mob.getPathfinder().moveTo(Objects.requireNonNull(path, "path"), speed);
    }

    @Override
    public boolean hasPath() {
        return this.mob.getPathfinder().hasPath();
    }

    @Nullable
    @Override
    public Pathfinder.PathResult currentPath() {
        return this.mob.getPathfinder().getCurrentPath();
    }

    @Override
    public void stop() {
        this.mob.getPathfinder().stopPathfinding();
    }

    @Override
    public void applyNavigationProfile(@NotNull PathNavigationProfile profile) {
        Objects.requireNonNull(profile, "profile");
        Pathfinder pathfinder = this.mob.getPathfinder();
        if (profile.canOpenDoors() != null) {
            pathfinder.setCanOpenDoors(profile.canOpenDoors());
        }
        if (profile.canPassDoors() != null) {
            pathfinder.setCanPassDoors(profile.canPassDoors());
        }
        if (profile.canFloat() != null) {
            pathfinder.setCanFloat(profile.canFloat());
        }
    }
}
