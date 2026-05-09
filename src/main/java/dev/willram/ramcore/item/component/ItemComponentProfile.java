package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.datacomponent.item.Weapon;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * High-level builder for common item component patches.
 */
public final class ItemComponentProfile {
    private final ItemComponentPatch patch;

    private ItemComponentProfile(ItemComponentPatch patch) {
        this.patch = Objects.requireNonNull(patch, "patch");
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public ItemComponentPatch patch() {
        return this.patch;
    }

    public void apply(@NotNull ItemComponentBackend backend) {
        this.patch.apply(backend);
    }

    public static final class Builder {
        private final ItemComponentPatch.Builder patch = ItemComponentPatch.builder();

        public <T> Builder component(@NotNull DataComponentType.Valued<T> type, @NotNull T value) {
            this.patch.set(type, value);
            return this;
        }

        public Builder component(@NotNull DataComponentType.NonValued type) {
            this.patch.set(type);
            return this;
        }

        public Builder remove(@NotNull DataComponentType type) {
            this.patch.unset(type);
            return this;
        }

        public Builder reset(@NotNull DataComponentType type) {
            this.patch.reset(type);
            return this;
        }

        public Builder maxStackSize(int amount) {
            this.patch.set(DataComponentTypes.MAX_STACK_SIZE, amount);
            return this;
        }

        public Builder maxDamage(int damage) {
            this.patch.set(DataComponentTypes.MAX_DAMAGE, damage);
            return this;
        }

        public Builder damage(int damage) {
            this.patch.set(DataComponentTypes.DAMAGE, damage);
            return this;
        }

        public Builder itemName(@NotNull Component name) {
            this.patch.set(DataComponentTypes.ITEM_NAME, Objects.requireNonNull(name, "name"));
            return this;
        }

        public Builder customName(@NotNull Component name) {
            this.patch.set(DataComponentTypes.CUSTOM_NAME, Objects.requireNonNull(name, "name"));
            return this;
        }

        public Builder itemModel(@NotNull Key model) {
            this.patch.set(DataComponentTypes.ITEM_MODEL, Objects.requireNonNull(model, "model"));
            return this;
        }

        public Builder rarity(@NotNull ItemRarity rarity) {
            this.patch.set(DataComponentTypes.RARITY, Objects.requireNonNull(rarity, "rarity"));
            return this;
        }

        public Builder enchantmentGlintOverride(boolean glint) {
            this.patch.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
            return this;
        }

        public Builder glider(boolean enabled) {
            if (enabled) {
                this.patch.set(DataComponentTypes.GLIDER);
            } else {
                this.patch.unset(DataComponentTypes.GLIDER);
            }
            return this;
        }

        public Builder customModelData(@NotNull Consumer<CustomModelData.Builder> customizer) {
            CustomModelData.Builder builder = CustomModelData.customModelData();
            Objects.requireNonNull(customizer, "customizer").accept(builder);
            this.patch.set(DataComponentTypes.CUSTOM_MODEL_DATA, builder);
            return this;
        }

        public Builder food(int nutrition, float saturation, boolean canAlwaysEat) {
            this.patch.set(DataComponentTypes.FOOD, FoodProperties.food()
                    .nutrition(nutrition)
                    .saturation(saturation)
                    .canAlwaysEat(canAlwaysEat));
            return this;
        }

        public Builder tool(float defaultMiningSpeed, int damagePerBlock, boolean canDestroyBlocksInCreative) {
            this.patch.set(DataComponentTypes.TOOL, Tool.tool()
                    .defaultMiningSpeed(defaultMiningSpeed)
                    .damagePerBlock(damagePerBlock)
                    .canDestroyBlocksInCreative(canDestroyBlocksInCreative));
            return this;
        }

        public Builder weapon(int itemDamagePerAttack, float disableBlockingForSeconds) {
            this.patch.set(DataComponentTypes.WEAPON, Weapon.weapon()
                    .itemDamagePerAttack(itemDamagePerAttack)
                    .disableBlockingForSeconds(disableBlockingForSeconds));
            return this;
        }

        public Builder equippable(@NotNull EquipmentSlot slot, @NotNull Consumer<Equippable.Builder> customizer) {
            Equippable.Builder builder = Equippable.equippable(Objects.requireNonNull(slot, "slot"));
            Objects.requireNonNull(customizer, "customizer").accept(builder);
            this.patch.set(DataComponentTypes.EQUIPPABLE, builder);
            return this;
        }

        public Builder container(@NotNull List<ItemStack> items) {
            this.patch.set(DataComponentTypes.CONTAINER, ItemContainerContents.containerContents(items));
            return this;
        }

        public Builder bundle(@NotNull List<ItemStack> items) {
            this.patch.set(DataComponentTypes.BUNDLE_CONTENTS, BundleContents.bundleContents(items));
            return this;
        }

        public ItemComponentProfile build() {
            return new ItemComponentProfile(this.patch.build());
        }
    }
}
