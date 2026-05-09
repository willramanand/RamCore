package dev.willram.ramcore.path;

import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Entry point for starting managed path tasks for a mob.
 */
public final class PathController {
    private final PathBackend backend;

    PathController(@NotNull PathBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @NotNull
    public Mob mob() {
        return this.backend.mob();
    }

    @NotNull
    public PathBackend backend() {
        return this.backend;
    }

    @NotNull
    public PathTask create(@NotNull PathRequest request) {
        return new PathTask(this.backend, Objects.requireNonNull(request, "request"));
    }

    @NotNull
    public PathTask schedule(@NotNull PathRequest request) {
        return schedule(request, 1L);
    }

    @NotNull
    public PathTask schedule(@NotNull PathRequest request, long intervalTicks) {
        if (intervalTicks <= 0L) {
            throw new IllegalArgumentException("path task interval ticks must be greater than zero");
        }
        PathTask pathTask = create(request);
        Task schedulerTask = Schedulers.runTimer(this.backend.mob(), pathTask::tick, 0L, intervalTicks);
        pathTask.bind(schedulerTask);
        return pathTask;
    }
}
