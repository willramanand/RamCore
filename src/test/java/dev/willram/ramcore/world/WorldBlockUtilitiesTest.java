package dev.willram.ramcore.world;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import dev.willram.ramcore.serialize.BlockPosition;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.spawner.Spawner;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class WorldBlockUtilitiesTest {

    @Test
    public void spawnerConfigValidatesDelaysAndDescribesSettings() {
        assertThrows(IllegalArgumentException.class, () -> SpawnerConfig.builder()
                .minSpawnDelay(40)
                .maxSpawnDelay(20));

        SpawnerConfig config = SpawnerConfig.builder()
                .spawnedTemplate(SpawnerEntityTemplate.of(EntityType.ZOMBIE))
                .delay(5)
                .minSpawnDelay(20)
                .maxSpawnDelay(60)
                .spawnCount(3)
                .maxNearbyEntities(8)
                .requiredPlayerRange(12)
                .spawnRange(4)
                .build();

        assertEquals("minecraft:zombie", config.describe().get("spawnedType"));
        assertEquals("3", config.describe().get("spawnCount"));
    }

    @Test
    public void spawnerConfigAppliesToPaperSpawnerSurface() {
        CapturingSpawner handler = new CapturingSpawner();
        Spawner spawner = handler.proxy();
        SpawnerConfig config = SpawnerConfig.builder()
                .spawnedTemplate(SpawnerEntityTemplate.of(EntityType.ZOMBIE))
                .delay(5)
                .minSpawnDelay(20)
                .maxSpawnDelay(60)
                .spawnCount(3)
                .maxNearbyEntities(8)
                .requiredPlayerRange(12)
                .spawnRange(4)
                .build();

        config.apply(spawner);

        assertEquals(EntityType.ZOMBIE, handler.values.get("spawnedType"));
        assertEquals(5, handler.values.get("delay"));
        assertEquals(20, handler.values.get("minSpawnDelay"));
        assertEquals(60, handler.values.get("maxSpawnDelay"));
        assertEquals(3, handler.values.get("spawnCount"));
        assertEquals(8, handler.values.get("maxNearbyEntities"));
        assertEquals(12, handler.values.get("requiredPlayerRange"));
        assertEquals(4, handler.values.get("spawnRange"));
    }

    @Test
    public void weightedSpawnEntriesRequireSnapshotsForPaperConversion() {
        SpawnerWeightedEntry raw = SpawnerWeightedEntry.of(SpawnerEntityTemplate.rawSnbt(EntityType.ZOMBIE, "{id:\"minecraft:zombie\"}"), 2);

        assertThrows(UnsupportedOperationException.class, raw::toPaper);

        SpawnerWeightedEntry paper = SpawnerWeightedEntry.of(SpawnerEntityTemplate.snapshot(snapshot(EntityType.ZOMBIE)), 4);
        assertEquals(4, paper.toPaper().getSpawnWeight());
    }

    @Test
    public void structureSnapshotSingleChunkGuardRejectsCrossChunkRestores() {
        BlockPosition origin = BlockPosition.of(0, 64, 0, "world");
        StructureSnapshot snapshot = StructureSnapshot.builder(origin)
                .block(StructureBlockSnapshot.of(new BlockOffset(0, 0, 0), "minecraft:stone", "minecraft:stone"))
                .block(StructureBlockSnapshot.of(new BlockOffset(16, 0, 0), "minecraft:stone", "minecraft:stone"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> snapshot.validateSingleTargetChunk(origin));
    }

    @Test
    public void blockEntitySnapshotBuilderStoresTypedSurfaces() {
        BlockEntitySnapshot snapshot = BlockEntitySnapshot.builder(
                        BlockPosition.of(1, 64, 2, "world"),
                        "minecraft:oak_sign",
                        "minecraft:oak_sign[rotation=0]"
                )
                .kind(BlockEntityKind.SIGN)
                .pdcKey("example:id")
                .property("sign.front.line.0", "Hello")
                .rawNbt("{id:\"minecraft:sign\"}")
                .build();

        assertEquals(BlockEntityKind.SIGN, snapshot.kind());
        assertTrue(snapshot.pdcKeys().contains("example:id"));
        assertEquals("Hello", snapshot.properties().get("sign.front.line.0"));
        assertTrue(snapshot.rawNbt().isPresent());
    }

    @Test
    public void unsupportedBlockEntityNbtCodecIsExplicit() {
        BlockEntityNbtCodec codec = WorldBlocks.unsupportedNbtCodec();
        BlockEntityNbtResult<String> result = codec.exportSnbt(blockState());

        assertFalse(codec.supported());
        assertFalse(result.supported());
        assertTrue(result.value().isEmpty());
        assertTrue(result.message().contains("guarded NMS adapter"));
    }

    @Test
    public void worldBlocksCapabilityReportsPartialPaperSupport() {
        NmsAccessRegistry registry = WorldBlocks.registerPaperCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE)
        );

        assertEquals(NmsSupportStatus.PARTIAL, registry.check(NmsCapability.BLOCK_ENTITY_NBT).status());
    }

    private static EntitySnapshot snapshot(EntityType type) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getEntityType" -> type;
            case "getAsString" -> "{id:\"" + type.key().asString() + "\"}";
            case "toString" -> "Snapshot(" + type + ")";
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> null;
        };
        return (EntitySnapshot) Proxy.newProxyInstance(
                EntitySnapshot.class.getClassLoader(),
                new Class<?>[]{EntitySnapshot.class},
                handler
        );
    }

    private static BlockState blockState() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> "BlockState";
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> null;
        };
        return (BlockState) Proxy.newProxyInstance(
                BlockState.class.getClassLoader(),
                new Class<?>[]{BlockState.class},
                handler
        );
    }

    private static final class CapturingSpawner implements InvocationHandler {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final List<Object> potentialSpawns = new ArrayList<>();

        private Spawner proxy() {
            return (Spawner) Proxy.newProxyInstance(
                    Spawner.class.getClassLoader(),
                    new Class<?>[]{Spawner.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "setSpawnedType" -> set("spawnedType", args[0]);
                case "setDelay" -> set("delay", args[0]);
                case "setMinSpawnDelay" -> set("minSpawnDelay", args[0]);
                case "setMaxSpawnDelay" -> set("maxSpawnDelay", args[0]);
                case "setSpawnCount" -> set("spawnCount", args[0]);
                case "setMaxNearbyEntities" -> set("maxNearbyEntities", args[0]);
                case "setRequiredPlayerRange" -> set("requiredPlayerRange", args[0]);
                case "setSpawnRange" -> set("spawnRange", args[0]);
                case "setPotentialSpawns" -> setPotentialSpawns(args[0]);
                case "getPotentialSpawns" -> List.of();
                case "toString" -> "CapturingSpawner";
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> defaultValue(method.getReturnType());
            };
        }

        private Object set(String key, Object value) {
            this.values.put(key, value);
            return null;
        }

        @SuppressWarnings("unchecked")
        private Object setPotentialSpawns(Object value) {
            this.potentialSpawns.clear();
            this.potentialSpawns.addAll((List<Object>) value);
            return null;
        }
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
