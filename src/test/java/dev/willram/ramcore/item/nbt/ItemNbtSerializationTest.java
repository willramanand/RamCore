package dev.willram.ramcore.item.nbt;

import dev.willram.ramcore.item.component.ItemComponentSnapshot;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ItemNbtSerializationTest {

    @Test
    public void itemDiffReportsChangedSurfaces() {
        DataComponentType.Valued<Integer> damage = valued("damage");
        ItemSnapshot before = ItemSnapshot.builder("minecraft:diamond_sword", 1)
                .meta(Map.of("display-name", "Old"))
                .pdc(Set.of("example:id"))
                .components(new ItemComponentSnapshot(Map.of(damage, 0), Set.of(), Set.of(damage)))
                .enchantments(Map.of("minecraft:sharpness", 3))
                .attributes(Map.of("minecraft:generic.attack_damage:example:bonus", "2.0:ADD_NUMBER"))
                .rawNbt("{old:1b}")
                .build();
        ItemSnapshot after = ItemSnapshot.builder("minecraft:diamond_sword", 2)
                .meta(Map.of("display-name", "New"))
                .pdc(Set.of("example:id", "example:tier"))
                .components(new ItemComponentSnapshot(Map.of(damage, 12), Set.of(), Set.of(damage)))
                .enchantments(Map.of("minecraft:sharpness", 5))
                .attributes(Map.of("minecraft:generic.attack_damage:example:bonus", "3.0:ADD_NUMBER"))
                .rawNbt("{new:1b}")
                .build();

        ItemDiff diff = before.diff(after);

        assertTrue(diff.changed(ItemDiffSection.AMOUNT));
        assertTrue(diff.changed(ItemDiffSection.META));
        assertTrue(diff.changed(ItemDiffSection.PDC));
        assertTrue(diff.changed(ItemDiffSection.DATA_COMPONENTS));
        assertTrue(diff.changed(ItemDiffSection.ENCHANTMENTS));
        assertTrue(diff.changed(ItemDiffSection.ATTRIBUTES));
        assertTrue(diff.changed(ItemDiffSection.RAW_NBT));
        assertFalse(diff.changed(ItemDiffSection.TYPE));
    }

    @Test
    public void itemDiffCanIgnoreSections() {
        ItemSnapshot before = ItemSnapshot.builder("minecraft:stone", 1).build();
        ItemSnapshot after = ItemSnapshot.builder("minecraft:stone", 64)
                .meta(Map.of("name", "Stack"))
                .build();

        ItemDiff diff = ItemDiff.between(before, after, EnumSet.of(ItemDiffSection.AMOUNT, ItemDiffSection.META));

        assertTrue(diff.empty());
    }

    @Test
    public void unsupportedSnbtCodecIsExplicitAndSafe() {
        ItemSnbtCodec codec = ItemNbt.unsupportedSnbtCodec();

        ItemSnbtResult<ItemStack> imported = codec.importSnbt("{id:\"minecraft:stone\"}");

        assertFalse(codec.supported());
        assertFalse(imported.supported());
        assertTrue(imported.value().isEmpty());
        assertTrue(imported.message().contains("guarded NMS adapter"));
    }

    @Test
    public void customItemIdentityIsNamespacedAndVersioned() {
        CustomItemIdentity identity = new CustomItemIdentity(NamespacedKey.minecraft("ruby_sword"), 3);

        assertEquals("minecraft:ruby_sword@3", identity.toString());
        assertEquals(identity, new CustomItemIdentity(NamespacedKey.minecraft("ruby_sword"), 3));
    }

    @Test
    public void itemNbtCapabilityReportsPartialPaperSupport() {
        NmsAccessRegistry registry = ItemNbt.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.ITEM_NBT).status());
    }

    @SuppressWarnings("unchecked")
    private static <T> DataComponentType.Valued<T> valued(String key) {
        NamespacedKey namespacedKey = NamespacedKey.minecraft(key);
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getKey" -> namespacedKey;
            case "isPersistent" -> true;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> namespacedKey.toString();
            default -> defaultValue(method.getReturnType());
        };
        return (DataComponentType.Valued<T>) Proxy.newProxyInstance(
                DataComponentType.Valued.class.getClassLoader(),
                new Class<?>[]{DataComponentType.Valued.class},
                handler
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
