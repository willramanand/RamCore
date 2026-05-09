package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MobAiFacadeTest {

    @Test
    public void controllerCanAddPauseResumeSnapshotAndRestoreGoals() {
        Mob mob = mob("mob");
        MobGoalBackend backend = MobAi.memoryBackend();
        MobAiController<Mob> controller = MobAi.controller(mob, backend);
        RamMobGoal<Mob> one = goal("one", GoalType.MOVE);
        RamMobGoal<Mob> two = goal("two", GoalType.LOOK);

        controller.add(1, one).add(2, two);
        MobGoalSnapshot<Mob> snapshot = controller.snapshot();

        assertTrue(controller.has(one.getKey()));
        controller.pause(one.getKey());
        assertFalse(controller.has(one.getKey()));
        controller.resume(one.getKey());
        assertTrue(controller.has(one.getKey()));

        controller.removeAll();
        assertFalse(controller.has(one.getKey()));
        controller.restore(snapshot);

        assertTrue(controller.has(one.getKey()));
        assertTrue(controller.has(two.getKey()));
    }

    @Test
    public void diagnosticsReportConflictsRunningGoalsAndTarget() {
        AtomicReference<LivingEntity> target = new AtomicReference<>(living("target"));
        Mob mob = mob("mob", target);
        MobAiController<Mob> controller = MobAi.controller(mob, MobAi.memoryBackend());

        controller.add(5, goal("move_a", GoalType.MOVE));
        controller.add(5, goal("move_b", GoalType.MOVE));

        MobAiDiagnostics<Mob> diagnostics = controller.diagnostics();

        assertEquals(1, diagnostics.conflicts().size());
        assertEquals(2, diagnostics.runningGoals().size());
        assertEquals(target.get().getUniqueId(), diagnostics.targetId().orElseThrow());
    }

    @Test
    public void ramGoalTracksLifecycleDebugState() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-08T12:00:00Z"), ZoneOffset.UTC);
        RamMobGoal<Mob> goal = MobAi.goal(key("debug"))
                .types(GoalType.UNKNOWN_BEHAVIOR)
                .clock(clock)
                .build();

        goal.start();
        goal.tick();
        goal.tick();
        goal.stop();

        assertEquals(MobGoalDebugStatus.STOPPED, goal.debugState().status());
        assertEquals(1, goal.debugState().activations());
        assertEquals(2, goal.debugState().ticks());
        assertEquals(clock.instant(), goal.debugState().lastActivatedAt().orElseThrow());
        assertEquals(clock.instant(), goal.debugState().lastStoppedAt().orElseThrow());
    }

    @Test
    public void attackTargetGoalUpdatesAndClearsMobTarget() {
        AtomicReference<LivingEntity> target = new AtomicReference<>(living("target"));
        Mob mob = mob("mob");
        RamMobGoal<Mob> goal = CommonMobGoals.attackTarget(mob, MobAi.goal(key("attack")), target::get);

        goal.start();
        assertEquals(target.get(), mob.getTarget());

        goal.stop();
        assertEquals(null, mob.getTarget());
    }

    @Test
    public void paperCapabilityCanBeRegisteredWithNmsStrategy() {
        NmsAccessRegistry registry = MobAi.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.SUPPORTED, registry.check(NmsCapability.MOB_GOALS).status());
    }

    private static RamMobGoal<Mob> goal(String key, GoalType type) {
        return MobAi.goal(key(key)).types(type).build();
    }

    private static GoalKey<Mob> key(String key) {
        return GoalKey.of(Mob.class, NamespacedKey.minecraft(key));
    }

    private static Mob mob(String name) {
        return mob(name, new AtomicReference<>());
    }

    private static Mob mob(String name, AtomicReference<LivingEntity> target) {
        World world = world("world");
        Pathfinder pathfinder = pathfinder();
        UUID uuid = UUID.nameUUIDFromBytes(("mob:" + name).getBytes());
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> name;
            case "getWorld" -> world;
            case "getLocation" -> new Location(world, 0, 64, 0);
            case "getPathfinder" -> pathfinder;
            case "setTarget" -> {
                target.set((LivingEntity) args[0]);
                yield null;
            }
            case "getTarget" -> target.get();
            case "lookAt", "setRotation" -> null;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Mob{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Mob.class, handler);
    }

    private static LivingEntity living(String name) {
        World world = world("world");
        UUID uuid = UUID.nameUUIDFromBytes(("living:" + name).getBytes());
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> name;
            case "getWorld" -> world;
            case "getLocation" -> new Location(world, 1, 64, 1);
            case "hasLineOfSight" -> true;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Living{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(LivingEntity.class, handler);
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

    private static Pathfinder pathfinder() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "moveTo" -> true;
            case "getPoints" -> List.of();
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Pathfinder.class, handler);
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
