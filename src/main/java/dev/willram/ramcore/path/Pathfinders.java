package dev.willram.ramcore.path;

import com.destroystokyo.paper.entity.ai.GoalType;
import dev.willram.ramcore.ai.RamMobGoal;
import dev.willram.ramcore.ai.RamMobGoalBuilder;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Static factory methods for managed pathfinding.
 */
public final class Pathfinders {

    @NotNull
    public static PathController controller(@NotNull Mob mob) {
        return new PathController(paperBackend(mob));
    }

    @NotNull
    public static PathController controller(@NotNull PathBackend backend) {
        return new PathController(backend);
    }

    @NotNull
    public static PathBackend paperBackend(@NotNull Mob mob) {
        return new PaperPathBackend(mob);
    }

    @NotNull
    public static InMemoryPathBackend memoryBackend(@NotNull Mob mob, @NotNull Location currentLocation) {
        return new InMemoryPathBackend(mob, currentLocation);
    }

    @NotNull
    public static PathRequest.Builder to(@NotNull Location location) {
        return PathRequest.to(location);
    }

    @NotNull
    public static PathRequest.Builder to(@NotNull Entity entity) {
        return PathRequest.to(entity);
    }

    @NotNull
    public static WaypointRoute.Builder route() {
        return WaypointRoute.builder();
    }

    @NotNull
    public static PatrolController patrol(@NotNull PathController controller, @NotNull WaypointRoute route,
                                          @NotNull PathOptions options) {
        return patrol(controller, route, options, ignored -> {});
    }

    @NotNull
    public static PatrolController patrol(@NotNull PathController controller, @NotNull WaypointRoute route,
                                          @NotNull PathOptions options,
                                          @NotNull Consumer<PathTaskResult> terminalConsumer) {
        return new PatrolController(controller, route, options, terminalConsumer);
    }

    @NotNull
    public static <T extends Mob> RamMobGoal<T> patrolGoal(@NotNull T mob, @NotNull RamMobGoalBuilder<T> builder,
                                                           @NotNull WaypointRoute route, @NotNull PathOptions options) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(options, "options");
        PathController controller = controller(mob);
        AtomicInteger index = new AtomicInteger();
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicReference<PathTask> task = new AtomicReference<>();
        return builder.types(GoalType.MOVE)
                .activateWhen(() -> !finished.get())
                .stayActiveWhen(() -> !finished.get())
                .onStart(() -> task.set(createRouteTask(controller, route, options, index, finished, task)))
                .onTick(() -> {
                    PathTask current = task.get();
                    if (current != null && current.active()) {
                        current.tick();
                    }
                })
                .onStop(() -> {
                    PathTask current = task.getAndSet(null);
                    if (current != null && current.active()) {
                        current.cancel(PathCancelReason.MANUAL);
                    }
                })
                .build();
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.PATHFINDING,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper-pathfinder",
                null,
                null,
                "Paper exposes movement, path points, door toggles, and float toggles; node penalties and movement controllers need a versioned adapter."
        ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.PATHFINDING_ROUTES,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-path-routes",
                        "RamCore provides waypoint routes and patrol controllers on top of managed path requests."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.PATHFINDING_NAVIGATION_PROFILE,
                        NmsAccessTier.PAPER_API,
                        "paper-pathfinder",
                        "Paper exposes door and floating navigation toggles; node penalties and navigation type selection need an adapter."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.PATHFINDING_NODE_CONTROL,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Paper does not expose node penalties or low-level navigation internals."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.ENTITY_MOVEMENT_CONTROLLERS,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Raw move, look, jump, flying, and swimming controllers require versioned NMS adapters."
                ));
        return registry;
    }

    private static PathTask createRouteTask(PathController controller, WaypointRoute route, PathOptions options,
                                            AtomicInteger index, AtomicBoolean finished,
                                            AtomicReference<PathTask> task) {
        PathRequest request = copyOptions(PathRequest.to(route.waypoint(index.get())), options)
                .onComplete(result -> {
                    if (route.hasNext(index.get())) {
                        index.incrementAndGet();
                        task.set(createRouteTask(controller, route, options, index, finished, task));
                    } else {
                        finished.set(true);
                    }
                })
                .onFailure(result -> finished.set(true))
                .onCancel(result -> finished.set(true))
                .build();
        return controller.create(request);
    }

    private static PathRequest.Builder copyOptions(PathRequest.Builder builder, PathOptions options) {
        return builder.speed(options.speed())
                .maxDistance(options.maxDistance())
                .stuckTimeoutTicks(options.stuckTimeoutTicks())
                .repathIntervalTicks(options.repathIntervalTicks())
                .completionDistance(options.completionDistance())
                .timeoutTicks(options.timeoutTicks())
                .stuckDistance(options.stuckDistance())
                .navigationProfile(options.navigationProfile());
    }

    private Pathfinders() {
    }
}
