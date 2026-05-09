package dev.willram.ramcore.path;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PathfindingFacadeTest {

    @Test
    public void taskRepathsTracksProgressAndCompletes() {
        World world = world("world");
        Mob mob = mob("path");
        Location start = new Location(world, 0, 64, 0);
        Location destination = new Location(world, 5, 64, 0);
        InMemoryPathBackend backend = Pathfinders.memoryBackend(mob, start)
                .path(start, destination);
        List<PathProgress> progress = new ArrayList<>();
        AtomicReference<PathTaskResult> completed = new AtomicReference<>();

        PathTask task = Pathfinders.controller(backend).create(Pathfinders.to(destination)
                .speed(1.3d)
                .repathIntervalTicks(1L)
                .stuckTimeoutTicks(20L)
                .completionDistance(0.25d)
                .onProgress(progress::add)
                .onComplete(completed::set)
                .build());

        task.tick();
        backend.currentLocation(destination);
        task.tick();

        assertEquals(PathStatus.COMPLETE, task.status());
        assertEquals(1, backend.moveAttempts());
        assertEquals(PathStatus.RUNNING, progress.get(0).status());
        assertEquals(destination, completed.get().progress().destination());
        assertTrue(completed.get().successful());
    }

    @Test
    public void taskFailsWhenEntityIsStuck() {
        World world = world("world");
        Location start = new Location(world, 0, 64, 0);
        InMemoryPathBackend backend = Pathfinders.memoryBackend(mob("stuck"), start)
                .path(start, new Location(world, 10, 64, 0));
        AtomicReference<PathTaskResult> failed = new AtomicReference<>();

        PathTask task = Pathfinders.controller(backend).create(Pathfinders.to(new Location(world, 10, 64, 0))
                .repathIntervalTicks(1L)
                .stuckTimeoutTicks(2L)
                .onFailure(failed::set)
                .build());

        task.tick();
        task.tick();

        assertEquals(PathStatus.FAILED, task.status());
        assertEquals(PathFailureReason.STUCK, failed.get().failureReason());
        assertTrue(failed.get().progress().stuck());
    }

    @Test
    public void taskReportsRejectedMoveAndAppliesNavigationProfile() {
        World world = world("world");
        Location start = new Location(world, 0, 64, 0);
        PathNavigationProfile profile = PathNavigationProfile.builder()
                .canOpenDoors(true)
                .canPassDoors(false)
                .canFloat(true)
                .build();
        InMemoryPathBackend backend = Pathfinders.memoryBackend(mob("rejected"), start)
                .path(start, new Location(world, 4, 64, 0))
                .moveAccepted(false);
        AtomicReference<PathTaskResult> failed = new AtomicReference<>();

        PathTask task = Pathfinders.controller(backend).create(Pathfinders.to(new Location(world, 4, 64, 0))
                .navigationProfile(profile)
                .onFailure(failed::set)
                .build());

        task.tick();

        assertEquals(PathFailureReason.MOVE_REJECTED, failed.get().failureReason());
        assertEquals(profile, backend.lastProfile());
        assertFalse(backend.hasPath());
    }

    @Test
    public void taskCanBeCancelledWithReason() {
        World world = world("world");
        InMemoryPathBackend backend = Pathfinders.memoryBackend(mob("cancel"), new Location(world, 0, 64, 0));
        AtomicReference<PathTaskResult> cancelled = new AtomicReference<>();
        PathTask task = Pathfinders.controller(backend).create(Pathfinders.to(new Location(world, 2, 64, 0))
                .onCancel(cancelled::set)
                .build());

        assertTrue(task.cancel(PathCancelReason.REPLACED));

        assertEquals(PathStatus.CANCELLED, task.status());
        assertEquals(PathCancelReason.REPLACED, cancelled.get().cancelReason());
    }

    @Test
    public void pathfindingCapabilityReportsPartialPaperSupport() {
        NmsAccessRegistry registry = Pathfinders.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.PATHFINDING).status());
    }

    private static Mob mob(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("mob:" + name).getBytes());
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Mob{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Mob.class, handler);
    }

    private static World world(String name) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "World{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(World.class, handler);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
