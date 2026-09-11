package dev.willram.ramcore.packet;

import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Per-viewer packet visual state. This state is not server authority.
 */
public final class PacketVisualState {
    private final Map<Integer, Map<String, Object>> metadataPreviews = new LinkedHashMap<>();
    private final Set<Integer> glowingEntities = new LinkedHashSet<>();
    private final Map<Integer, Map<EquipmentSlot, Object>> equipmentPreviews = new LinkedHashMap<>();
    private final Map<Integer, PacketFakeEntity> fakeEntities = new LinkedHashMap<>();

    @NotNull
    public PacketVisualOperation metadataPreview(int entityId, @NotNull String key, @NotNull Object value) {
        metadataPreviews.computeIfAbsent(validateEntityId(entityId), ignored -> new LinkedHashMap<>())
                .put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return PacketVisualOperation.of(PacketVisualAction.METADATA_PREVIEW, entityId, Map.of(key, value));
    }

    @NotNull
    public PacketVisualOperation clearMetadataPreview(int entityId) {
        metadataPreviews.remove(validateEntityId(entityId));
        return PacketVisualOperation.of(PacketVisualAction.CLEAR_METADATA_PREVIEW, entityId, Map.of());
    }

    @NotNull
    public PacketVisualOperation glowing(int entityId, boolean glowing) {
        validateEntityId(entityId);
        if (glowing) {
            glowingEntities.add(entityId);
        } else {
            glowingEntities.remove(entityId);
        }
        return PacketVisualOperation.of(PacketVisualAction.GLOWING, entityId, Map.of("glowing", glowing));
    }

    @NotNull
    public PacketVisualOperation equipment(int entityId, @NotNull EquipmentSlot slot, @NotNull Object itemView) {
        equipmentPreviews.computeIfAbsent(validateEntityId(entityId), ignored -> new LinkedHashMap<>())
                .put(Objects.requireNonNull(slot, "slot"), Objects.requireNonNull(itemView, "itemView"));
        return PacketVisualOperation.of(PacketVisualAction.EQUIPMENT, entityId, Map.of("slot", slot.name(), "item", itemView));
    }

    @NotNull
    public PacketVisualOperation clearEquipment(int entityId) {
        equipmentPreviews.remove(validateEntityId(entityId));
        return PacketVisualOperation.of(PacketVisualAction.CLEAR_EQUIPMENT, entityId, Map.of());
    }

    @NotNull
    public PacketVisualOperation spawnFake(@NotNull PacketFakeEntity fakeEntity) {
        Objects.requireNonNull(fakeEntity, "fakeEntity");
        fakeEntities.put(fakeEntity.entityId(), fakeEntity);
        return PacketVisualOperation.of(PacketVisualAction.SPAWN_FAKE_ENTITY, fakeEntity.entityId(), Map.of("fakeEntity", fakeEntity));
    }

    @NotNull
    public PacketVisualOperation destroyFake(int entityId) {
        fakeEntities.remove(validateEntityId(entityId));
        return PacketVisualOperation.of(PacketVisualAction.DESTROY_FAKE_ENTITY, entityId, Map.of());
    }

    @NotNull
    public PacketVisualOperation scoreboardVisual(@NotNull String id, @NotNull Map<String, Object> data) {
        Objects.requireNonNull(id, "id");
        return PacketVisualOperation.of(PacketVisualAction.SCOREBOARD_VISUAL, null, withId(id, data));
    }

    @NotNull
    public PacketVisualOperation reset() {
        metadataPreviews.clear();
        glowingEntities.clear();
        equipmentPreviews.clear();
        fakeEntities.clear();
        return PacketVisualOperation.of(PacketVisualAction.RESET_VIEWER, null, Map.of());
    }

    @NotNull
    public Optional<Object> metadata(int entityId, @NotNull String key) {
        return Optional.ofNullable(metadataPreviews.getOrDefault(entityId, Map.of()).get(Objects.requireNonNull(key, "key")));
    }

    public boolean glowing(int entityId) {
        return this.glowingEntities.contains(entityId);
    }

    @NotNull
    public Optional<Object> equipment(int entityId, @NotNull EquipmentSlot slot) {
        return Optional.ofNullable(equipmentPreviews.getOrDefault(entityId, Map.of()).get(Objects.requireNonNull(slot, "slot")));
    }

    @NotNull
    public Optional<PacketFakeEntity> fakeEntity(int entityId) {
        return Optional.ofNullable(fakeEntities.get(entityId));
    }

    private static int validateEntityId(int entityId) {
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
        return entityId;
    }

    private static Map<String, Object> withId(String id, Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>(Objects.requireNonNull(data, "data"));
        result.put("id", id);
        return result;
    }
}
