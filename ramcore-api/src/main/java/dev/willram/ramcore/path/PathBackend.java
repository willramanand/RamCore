package dev.willram.ramcore.path;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Backend for pathfinding operations. Public path APIs should target this
 * contract instead of raw NMS navigation handles.
 */
public interface PathBackend {

    @NotNull
    Mob mob();

    @NotNull
    default UUID entityId() {
        return mob().getUniqueId();
    }

    @NotNull
    default Location currentLocation() {
        return mob().getLocation();
    }

    @Nullable
    Pathfinder.PathResult findPath(@NotNull Location location);

    @Nullable
    Pathfinder.PathResult findPath(@NotNull Entity entity);

    boolean moveTo(@NotNull Pathfinder.PathResult path, double speed);

    boolean hasPath();

    @Nullable
    Pathfinder.PathResult currentPath();

    void stop();

    void applyNavigationProfile(@NotNull PathNavigationProfile profile);
}
