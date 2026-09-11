package dev.willram.ramcore.path;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered path route for patrols and scripted movement.
 */
public final class WaypointRoute {
    private final List<Location> waypoints;
    private final boolean loop;

    private WaypointRoute(Builder builder) {
        if (builder.waypoints.isEmpty()) {
            throw new IllegalArgumentException("route must contain at least one waypoint");
        }
        this.waypoints = builder.waypoints.stream().map(Location::clone).toList();
        this.loop = builder.loop;
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public List<Location> waypoints() {
        return this.waypoints.stream().map(Location::clone).toList();
    }

    public boolean loop() {
        return this.loop;
    }

    public boolean hasNext(int index) {
        return this.loop || index + 1 < this.waypoints.size();
    }

    @NotNull
    public Location waypoint(int index) {
        int size = this.waypoints.size();
        if (this.loop) {
            return this.waypoints.get(Math.floorMod(index, size)).clone();
        }
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
        return this.waypoints.get(index).clone();
    }

    public static final class Builder {
        private final List<Location> waypoints = new ArrayList<>();
        private boolean loop;

        public Builder add(@NotNull Location location) {
            this.waypoints.add(Objects.requireNonNull(location, "location").clone());
            return this;
        }

        public Builder addAll(@NotNull List<Location> locations) {
            Objects.requireNonNull(locations, "locations").forEach(this::add);
            return this;
        }

        public Builder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public WaypointRoute build() {
            return new WaypointRoute(this);
        }
    }
}
