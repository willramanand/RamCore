package dev.willram.ramcore.brain;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.memory.MemoryKey;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MobBrainFacadeTest {

    @Test
    public void controllerReadsWritesAndClearsTypedMemories() {
        LivingEntity entity = entity("brain");
        MobBrainController brain = MobBrains.controller(entity, MobBrains.memoryBackend());
        UUID target = UUID.randomUUID();

        brain.angryAt(target).attackCooldown(20);

        assertEquals(target, brain.get(MemoryKey.ANGRY_AT).orElseThrow());
        assertEquals(Integer.valueOf(20), brain.get(MemoryKey.ATTACK_TARGET_COOLDOWN).orElseThrow());

        brain.clear(MemoryKey.ANGRY_AT);

        assertFalse(brain.has(MemoryKey.ANGRY_AT));
    }

    @Test
    public void snapshotTracksPresentAndAbsentSelectedKeys() {
        LivingEntity entity = entity("snapshot");
        Location home = new Location(world("world"), 1, 64, 2);
        MobBrainController brain = MobBrains.controller(entity, MobBrains.memoryBackend())
                .home(home);

        BrainMemorySnapshot snapshot = brain.snapshot(List.of(MemoryKey.HOME, MemoryKey.JOB_SITE));

        assertEquals(home, snapshot.get(MemoryKey.HOME).orElseThrow());
        assertTrue(snapshot.absent().contains(MemoryKey.JOB_SITE));
        assertTrue(snapshot.presentKeys().contains(MemoryKey.HOME.getKey()));
    }

    @Test
    public void diagnosticsReportMemoryPartialAndSensorActivityUnsupported() {
        LivingEntity entity = entity("diagnostics");
        MobBrainDiagnostics diagnostics = MobBrains.controller(entity, MobBrains.memoryBackend())
                .diagnostics(MobBrains.COMMON_DEBUG_KEYS);

        assertEquals(entity.getUniqueId(), diagnostics.entityId());
        assertEquals(NmsSupportStatus.PARTIAL, diagnostics.memoryCapability().status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, diagnostics.sensorCapability().status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, diagnostics.activityCapability().status());
        assertTrue(diagnostics.lines().stream().anyMatch(line -> line.contains("BRAIN_MEMORY=PARTIAL")));
    }

    @Test
    public void nmsRegistryCanExposeBrainCapabilityDecisions() {
        NmsAccessRegistry registry = MobBrains.registerPaperCapabilities(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.BRAIN_MEMORY).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.BRAIN_SENSORS).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, registry.check(NmsCapability.BRAIN_ACTIVITY).status());
    }

    private static LivingEntity entity(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("living:" + name).getBytes());
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> name;
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
