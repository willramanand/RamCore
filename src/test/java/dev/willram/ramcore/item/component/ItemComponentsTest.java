package dev.willram.ramcore.item.component;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.NamespacedKey;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ItemComponentsTest {

    @Test
    public void patchCanSetUnsetAndResetComponents() {
        DataComponentType.Valued<Integer> maxStack = valued("max_stack");
        DataComponentType.NonValued glider = marker("glider");
        InMemoryItemComponentBackend backend = ItemComponents.memoryBackend()
                .prototype(maxStack, 64)
                .prototype(glider);

        ItemComponents.patch()
                .set(maxStack, 16)
                .unset(glider)
                .build()
                .apply(backend);

        assertEquals(Integer.valueOf(16), backend.get(maxStack));
        assertFalse(backend.has(glider));
        assertTrue(backend.overridden(maxStack));

        ItemComponents.patch()
                .reset(maxStack)
                .reset(glider)
                .build()
                .apply(backend);

        assertEquals(Integer.valueOf(64), backend.get(maxStack));
        assertTrue(backend.has(glider));
        assertFalse(backend.overridden(maxStack));
    }

    @Test
    public void snapshotsCanDiffAndApplyToAnotherBackend() {
        DataComponentType.Valued<Integer> damage = valued("damage");
        DataComponentType.Valued<String> model = valued("model");
        InMemoryItemComponentBackend before = ItemComponents.memoryBackend()
                .prototype(damage, 0);
        InMemoryItemComponentBackend after = ItemComponents.memoryBackend()
                .prototype(damage, 0);

        ItemComponents.patch()
                .set(damage, 12)
                .set(model, "example:sword")
                .build()
                .apply(after);

        ItemComponentPatch diff = before.snapshot().diff(after.snapshot());
        diff.apply(before);

        assertEquals(after.snapshot().values(), before.snapshot().values());
        assertEquals(Integer.valueOf(12), before.get(damage));
        assertEquals("example:sword", before.get(model));
    }

    @Test
    public void copyAndMatchesCanIgnoreSelectedComponents() {
        DataComponentType.Valued<Integer> damage = valued("damage");
        DataComponentType.Valued<String> name = valued("name");
        InMemoryItemComponentBackend source = ItemComponents.memoryBackend();
        InMemoryItemComponentBackend target = ItemComponents.memoryBackend();

        ItemComponents.patch()
                .set(damage, 5)
                .set(name, "Blade")
                .build()
                .apply(source);

        ItemComponents.copy(source, target, type -> type != damage);

        assertFalse(target.has(damage));
        assertEquals("Blade", target.get(name));
        assertTrue(ItemComponents.matchesIgnoring(source, target, Set.of(damage)));
        assertFalse(ItemComponents.matchesIgnoring(source, target, Set.of()));
    }

    @Test
    public void profileBuildsPatchAndSerializedPatchHasVersionMetadata() {
        DataComponentType.Valued<Integer> maxStack = valued("max_stack");
        DataComponentType.NonValued glider = marker("glider");
        InMemoryItemComponentBackend backend = ItemComponents.memoryBackend();

        ItemComponentProfile profile = ItemComponents.profile()
                .component(maxStack, 1)
                .component(glider)
                .build();
        profile.apply(backend);

        ItemComponentSerializedPatch serialized = ItemComponents.serialize(profile.patch());

        assertEquals(Integer.valueOf(1), backend.get(maxStack));
        assertTrue(backend.has(glider));
        assertEquals(ItemComponentSerializedPatch.CURRENT_FORMAT, serialized.formatVersion());
        assertEquals(2, serialized.changes().size());
        assertEquals("minecraft:max_stack", serialized.changes().get(0).component());
    }

    @Test
    public void itemDataComponentsCapabilityReportsPartialPaperSupport() {
        NmsAccessRegistry registry = ItemComponents.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.ITEM_DATA_COMPONENTS).status());
    }

    @SuppressWarnings("unchecked")
    private static <T> DataComponentType.Valued<T> valued(String key) {
        return (DataComponentType.Valued<T>) component(key, DataComponentType.Valued.class);
    }

    private static DataComponentType.NonValued marker(String key) {
        return component(key, DataComponentType.NonValued.class);
    }

    @SuppressWarnings("unchecked")
    private static <T extends DataComponentType> T component(String key, Class<T> type) {
        NamespacedKey namespacedKey = NamespacedKey.minecraft(key);
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getKey" -> namespacedKey;
            case "isPersistent" -> true;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> namespacedKey.toString();
            default -> defaultValue(method.getReturnType());
        };
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
        return null;
    }
}
