package dev.willram.ramcore.path;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reusable route runner that advances through waypoint path tasks.
 */
public final class PatrolController implements AutoCloseable {
    private final PathController controller;
    private final WaypointRoute route;
    private final PathOptions options;
    private final Consumer<PathTaskResult> terminalConsumer;

    private int index;
    private PathTask currentTask;
    private boolean closed;

    PatrolController(@NotNull PathController controller, @NotNull WaypointRoute route, @NotNull PathOptions options,
                     @NotNull Consumer<PathTaskResult> terminalConsumer) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.route = Objects.requireNonNull(route, "route");
        this.options = Objects.requireNonNull(options, "options");
        this.terminalConsumer = Objects.requireNonNull(terminalConsumer, "terminalConsumer");
    }

    @NotNull
    public PathTask start() {
        if (this.closed) {
            throw new IllegalStateException("patrol controller is closed");
        }
        this.currentTask = scheduleCurrent();
        return this.currentTask;
    }

    @NotNull
    public PathTask currentTask() {
        if (this.currentTask == null) {
            return start();
        }
        return this.currentTask;
    }

    public int index() {
        return this.index;
    }

    public boolean closed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        if (this.currentTask != null) {
            this.currentTask.cancel(PathCancelReason.SHUTDOWN);
        }
    }

    private PathTask scheduleCurrent() {
        PathRequest request = PathRequest.to(this.route.waypoint(this.index))
                .speed(this.options.speed())
                .maxDistance(this.options.maxDistance())
                .stuckTimeoutTicks(this.options.stuckTimeoutTicks())
                .repathIntervalTicks(this.options.repathIntervalTicks())
                .completionDistance(this.options.completionDistance())
                .timeoutTicks(this.options.timeoutTicks())
                .stuckDistance(this.options.stuckDistance())
                .navigationProfile(this.options.navigationProfile())
                .onComplete(this::advance)
                .onFailure(this::finish)
                .onCancel(this::finish)
                .build();
        return this.controller.schedule(request);
    }

    private void advance(PathTaskResult result) {
        if (this.closed) {
            return;
        }
        if (!this.route.hasNext(this.index)) {
            finish(result);
            return;
        }
        this.index++;
        this.currentTask = scheduleCurrent();
    }

    private void finish(PathTaskResult result) {
        this.closed = true;
        this.terminalConsumer.accept(result);
    }
}
