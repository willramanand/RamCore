package dev.willram.ramcore.path;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot of a managed path task at one tick.
 */
public record PathProgress(
        @NotNull UUID entityId,
        @NotNull PathStatus status,
        long ticksElapsed,
        @NotNull Location currentLocation,
        @Nullable Location destination,
        @Nullable Location nextPoint,
        @Nullable Location finalPoint,
        int nextPointIndex,
        int totalPoints,
        double remainingDistance,
        boolean stuck,
        boolean hasPath,
        boolean canReachFinalPoint,
        @Nullable PathFailureReason failureReason,
        @Nullable PathCancelReason cancelReason
) {
    public PathProgress {
        entityId = Objects.requireNonNull(entityId, "entityId");
        status = Objects.requireNonNull(status, "status");
        currentLocation = Objects.requireNonNull(currentLocation, "currentLocation").clone();
        destination = destination == null ? null : destination.clone();
        nextPoint = nextPoint == null ? null : nextPoint.clone();
        finalPoint = finalPoint == null ? null : finalPoint.clone();
    }

    @NotNull
    @Override
    public Location currentLocation() {
        return this.currentLocation.clone();
    }

    @Nullable
    @Override
    public Location destination() {
        return this.destination == null ? null : this.destination.clone();
    }

    @Nullable
    @Override
    public Location nextPoint() {
        return this.nextPoint == null ? null : this.nextPoint.clone();
    }

    @Nullable
    @Override
    public Location finalPoint() {
        return this.finalPoint == null ? null : this.finalPoint.clone();
    }

    public Optional<Location> destinationOptional() {
        return Optional.ofNullable(destination());
    }
}
