package dev.willram.ramcore.path;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Location or moving entity target for a path request.
 */
public final class PathDestination {
    private final Location location;
    private final Entity entity;

    private PathDestination(@Nullable Location location, @Nullable Entity entity) {
        this.location = location == null ? null : location.clone();
        this.entity = entity;
    }

    public static PathDestination location(@NotNull Location location) {
        return new PathDestination(Objects.requireNonNull(location, "location"), null);
    }

    public static PathDestination entity(@NotNull Entity entity) {
        return new PathDestination(null, Objects.requireNonNull(entity, "entity"));
    }

    @NotNull
    public PathDestinationType type() {
        return this.entity == null ? PathDestinationType.LOCATION : PathDestinationType.ENTITY;
    }

    @Nullable
    public Location fixedLocation() {
        return this.location == null ? null : this.location.clone();
    }

    @Nullable
    public Entity entity() {
        return this.entity;
    }

    @Nullable
    public Location currentLocation() {
        if (this.entity != null) {
            return this.entity.getLocation();
        }
        return fixedLocation();
    }
}
