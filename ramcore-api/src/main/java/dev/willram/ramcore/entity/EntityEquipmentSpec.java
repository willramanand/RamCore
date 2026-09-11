package dev.willram.ramcore.entity;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Equipment and drop chance values for a living entity.
 */
public final class EntityEquipmentSpec {
    private final Map<EquipmentSlot, ItemStack> items;
    private final Map<EquipmentSlot, Float> dropChances;

    private EntityEquipmentSpec(Map<EquipmentSlot, ItemStack> items, Map<EquipmentSlot, Float> dropChances) {
        this.items = copyItems(items);
        this.dropChances = Map.copyOf(dropChances);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void apply(@NotNull LivingEntity entity) {
        EntityEquipment equipment = Objects.requireNonNull(entity, "entity").getEquipment();
        if (equipment == null) {
            return;
        }
        for (Map.Entry<EquipmentSlot, ItemStack> entry : this.items.entrySet()) {
            equipment.setItem(entry.getKey(), entry.getValue().clone());
        }
        for (Map.Entry<EquipmentSlot, Float> entry : this.dropChances.entrySet()) {
            equipment.setDropChance(entry.getKey(), entry.getValue());
        }
    }

    @NotNull
    public Map<EquipmentSlot, ItemStack> items() {
        return copyItems(this.items);
    }

    @NotNull
    public Map<EquipmentSlot, Float> dropChances() {
        return this.dropChances;
    }

    private static Map<EquipmentSlot, ItemStack> copyItems(Map<EquipmentSlot, ItemStack> source) {
        Map<EquipmentSlot, ItemStack> copy = new EnumMap<>(EquipmentSlot.class);
        source.forEach((slot, item) -> copy.put(slot, item.clone()));
        return copy;
    }

    public static final class Builder {
        private final Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
        private final Map<EquipmentSlot, Float> dropChances = new EnumMap<>(EquipmentSlot.class);

        public Builder item(@NotNull EquipmentSlot slot, @NotNull ItemStack item) {
            this.items.put(Objects.requireNonNull(slot, "slot"), Objects.requireNonNull(item, "item").clone());
            return this;
        }

        public Builder dropChance(@NotNull EquipmentSlot slot, float chance) {
            if (chance < 0.0f || chance > 1.0f) {
                throw new IllegalArgumentException("equipment drop chance must be between 0 and 1");
            }
            this.dropChances.put(Objects.requireNonNull(slot, "slot"), chance);
            return this;
        }

        public EntityEquipmentSpec build() {
            return new EntityEquipmentSpec(this.items, this.dropChances);
        }
    }
}
