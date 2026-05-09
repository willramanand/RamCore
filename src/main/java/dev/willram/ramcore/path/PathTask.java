package dev.willram.ramcore.path;

import com.destroystokyo.paper.entity.Pathfinder;
import dev.willram.ramcore.scheduler.Task;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Managed, tick-driven path run. The task can be driven manually in tests or
 * bound to RamCore's entity scheduler by {@link PathController#schedule(PathRequest)}.
 */
public final class PathTask implements AutoCloseable {
    private final PathBackend backend;
    private final PathRequest request;

    private PathStatus status = PathStatus.PENDING;
    private long ticksElapsed;
    private long ticksSinceRepath = Long.MAX_VALUE;
    private long stuckTicks;
    private Location lastLocation;
    private PathProgress lastProgress;
    private Task scheduledTask;

    PathTask(@NotNull PathBackend backend, @NotNull PathRequest request) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.request = Objects.requireNonNull(request, "request");
        this.lastLocation = backend.currentLocation();
        this.lastProgress = snapshot(PathStatus.PENDING, false, null, null);
    }

    @NotNull
    public PathStatus status() {
        return this.status;
    }

    public boolean active() {
        return this.status == PathStatus.PENDING || this.status == PathStatus.RUNNING;
    }

    @NotNull
    public PathProgress progress() {
        return this.lastProgress;
    }

    @NotNull
    public PathRequest request() {
        return this.request;
    }

    @NotNull
    public PathBackend backend() {
        return this.backend;
    }

    void bind(@NotNull Task scheduledTask) {
        this.scheduledTask = Objects.requireNonNull(scheduledTask, "scheduledTask");
        if (!active()) {
            scheduledTask.stop();
        }
    }

    public void tick() {
        if (!active()) {
            stopScheduled();
            return;
        }

        if (this.status == PathStatus.PENDING) {
            this.backend.applyNavigationProfile(this.request.options().navigationProfile());
            this.status = PathStatus.RUNNING;
        }

        this.ticksElapsed++;

        Location destination = this.request.destination().currentLocation();
        if (destination == null) {
            fail(PathFailureReason.TARGET_UNAVAILABLE, "path target is no longer available");
            return;
        }

        if (!sameWorld(this.backend.currentLocation(), destination)) {
            fail(PathFailureReason.TARGET_UNAVAILABLE, "path target is in a different world");
            return;
        }

        double remainingDistance = distance(this.backend.currentLocation(), destination);
        PathOptions options = this.request.options();
        if (remainingDistance > options.maxDistance()) {
            fail(PathFailureReason.TOO_FAR, "path destination is outside the maximum distance");
            return;
        }

        if (remainingDistance <= options.completionDistance()) {
            complete("path destination reached");
            return;
        }

        if (this.ticksElapsed >= options.timeoutTicks()) {
            fail(PathFailureReason.TIMEOUT, "path timed out");
            return;
        }

        updateStuckState();
        if (this.stuckTicks >= options.stuckTimeoutTicks()) {
            fail(PathFailureReason.STUCK, "path entity is stuck");
            return;
        }

        if (this.ticksSinceRepath >= options.repathIntervalTicks()) {
            if (!repath(destination)) {
                return;
            }
            this.ticksSinceRepath = 0L;
        } else {
            this.ticksSinceRepath++;
        }

        this.lastProgress = snapshot(PathStatus.RUNNING, this.stuckTicks > 0L, null, null);
        this.request.progress(this.lastProgress);
    }

    public boolean cancel(@NotNull PathCancelReason reason) {
        Objects.requireNonNull(reason, "reason");
        if (!active()) {
            return false;
        }
        this.backend.stop();
        this.status = PathStatus.CANCELLED;
        this.lastProgress = snapshot(PathStatus.CANCELLED, false, null, reason);
        this.request.cancel(new PathTaskResult(PathStatus.CANCELLED, this.lastProgress, null, reason, "path cancelled: " + reason));
        stopScheduled();
        return true;
    }

    @Override
    public void close() {
        cancel(PathCancelReason.MANUAL);
    }

    private boolean repath(Location destination) {
        Pathfinder.PathResult path = switch (this.request.destination().type()) {
            case LOCATION -> this.backend.findPath(destination);
            case ENTITY -> findEntityPath();
        };
        if (path == null) {
            fail(PathFailureReason.NO_PATH, "no path was found");
            return false;
        }
        if (!this.backend.moveTo(path, this.request.options().speed())) {
            fail(PathFailureReason.MOVE_REJECTED, "path move was rejected by the backend");
            return false;
        }
        return true;
    }

    @Nullable
    private Pathfinder.PathResult findEntityPath() {
        Entity entity = this.request.destination().entity();
        if (entity == null) {
            return null;
        }
        return this.backend.findPath(entity);
    }

    private void updateStuckState() {
        Location current = this.backend.currentLocation();
        if (!sameWorld(current, this.lastLocation) || distance(current, this.lastLocation) > this.request.options().stuckDistance()) {
            this.stuckTicks = 0L;
            this.lastLocation = current;
            return;
        }
        this.stuckTicks++;
    }

    private void complete(String message) {
        this.backend.stop();
        this.status = PathStatus.COMPLETE;
        this.lastProgress = snapshot(PathStatus.COMPLETE, false, null, null);
        this.request.complete(new PathTaskResult(PathStatus.COMPLETE, this.lastProgress, null, null, message));
        stopScheduled();
    }

    private void fail(PathFailureReason reason, String message) {
        this.backend.stop();
        this.status = PathStatus.FAILED;
        this.lastProgress = snapshot(PathStatus.FAILED, reason == PathFailureReason.STUCK, reason, null);
        this.request.fail(new PathTaskResult(PathStatus.FAILED, this.lastProgress, reason, null, message));
        stopScheduled();
    }

    private void stopScheduled() {
        if (this.scheduledTask != null) {
            this.scheduledTask.stop();
            this.scheduledTask = null;
        }
    }

    private PathProgress snapshot(PathStatus status, boolean stuck, @Nullable PathFailureReason failureReason,
                                  @Nullable PathCancelReason cancelReason) {
        Location current = this.backend.currentLocation();
        Location destination = this.request.destination().currentLocation();
        Pathfinder.PathResult path = this.backend.currentPath();
        List<Location> points = path == null ? List.of() : path.getPoints();
        Location nextPoint = path == null ? null : path.getNextPoint();
        Location finalPoint = path == null ? null : path.getFinalPoint();
        return new PathProgress(
                this.backend.entityId(),
                status,
                this.ticksElapsed,
                current,
                destination,
                nextPoint,
                finalPoint,
                path == null ? -1 : path.getNextPointIndex(),
                points.size(),
                destination == null || !sameWorld(current, destination) ? Double.POSITIVE_INFINITY : distance(current, destination),
                stuck,
                this.backend.hasPath(),
                path != null && path.canReachFinalPoint(),
                failureReason,
                cancelReason
        );
    }

    private static boolean sameWorld(Location first, Location second) {
        return Objects.equals(first.getWorld(), second.getWorld());
    }

    private static double distance(Location first, Location second) {
        return first.distance(second);
    }
}
