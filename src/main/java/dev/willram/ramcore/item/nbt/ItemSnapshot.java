package dev.willram.ramcore.item.nbt;

import com.google.common.collect.Multimap;
import dev.willram.ramcore.item.component.ItemComponentSnapshot;
import dev.willram.ramcore.item.component.ItemComponents;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stable debug snapshot of the item surfaces RamCore can compare.
 */
public record ItemSnapshot(
        @NotNull String type,
        int amount,
        @NotNull Map<String, Object> meta,
        @NotNull Set<String> pdc,
        @NotNull ItemComponentSnapshot components,
        @NotNull Map<String, Integer> enchantments,
        @NotNull Map<String, String> attributes,
        @NotNull Optional<String> rawNbt
) {
    public ItemSnapshot {
        type = Objects.requireNonNull(type, "type");
        meta = Map.copyOf(Objects.requireNonNull(meta, "meta"));
        pdc = Set.copyOf(Objects.requireNonNull(pdc, "pdc"));
        components = Objects.requireNonNull(components, "components");
        enchantments = Map.copyOf(Objects.requireNonNull(enchantments, "enchantments"));
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
        rawNbt = Objects.requireNonNull(rawNbt, "rawNbt");
    }

    @NotNull
    public static ItemSnapshot capture(@NotNull ItemStack item) {
        return capture(item, new UnsupportedItemSnbtCodec("Raw item SNBT requires a guarded NMS adapter."));
    }

    @NotNull
    public static ItemSnapshot capture(@NotNull ItemStack item, @NotNull ItemSnbtCodec snbtCodec) {
        Objects.requireNonNull(item, "item");
        ItemMeta meta = item.getItemMeta();
        Optional<String> rawNbt = snbtCodec.exportSnbt(item).value();
        return builder(item.getType().getKey().toString(), item.getAmount())
                .meta(meta == null ? Map.of() : meta.serialize())
                .pdc(meta == null ? Set.of() : pdcKeys(meta))
                .components(ItemComponents.edit(item).snapshot())
                .enchantments(enchantments(item.getEnchantments()))
                .attributes(meta == null ? Map.of() : attributes(meta.getAttributeModifiers()))
                .rawNbt(rawNbt.orElse(null))
                .build();
    }

    @NotNull
    public static Builder builder(@NotNull String type, int amount) {
        return new Builder(type, amount);
    }

    @NotNull
    public ItemDiff diff(@NotNull ItemSnapshot after) {
        return ItemDiff.between(this, after);
    }

    private static Set<String> pdcKeys(ItemMeta meta) {
        Set<String> keys = new LinkedHashSet<>();
        meta.getPersistentDataContainer().getKeys().stream().map(NamespacedKey::toString).forEach(keys::add);
        return keys;
    }

    private static Map<String, Integer> enchantments(Map<Enchantment, Integer> source) {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        source.forEach((enchantment, level) -> enchantments.put(enchantment.getKey().toString(), level));
        return enchantments;
    }

    private static Map<String, String> attributes(@Nullable Multimap<Attribute, AttributeModifier> source) {
        if (source == null) {
            return Map.of();
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        source.forEach((attribute, modifier) -> attributes.put(attribute.getKey() + ":" + modifier.getKey(),
                modifier.getAmount() + ":" + modifier.getOperation()));
        return attributes;
    }

    public static final class Builder {
        private final String type;
        private final int amount;
        private Map<String, Object> meta = Map.of();
        private Set<String> pdc = Set.of();
        private ItemComponentSnapshot components = new ItemComponentSnapshot(Map.of(), Set.of(), Set.of());
        private Map<String, Integer> enchantments = Map.of();
        private Map<String, String> attributes = Map.of();
        private String rawNbt;

        private Builder(String type, int amount) {
            this.type = Objects.requireNonNull(type, "type");
            this.amount = amount;
        }

        public Builder meta(@NotNull Map<String, Object> meta) {
            this.meta = Objects.requireNonNull(meta, "meta");
            return this;
        }

        public Builder pdc(@NotNull Set<String> pdc) {
            this.pdc = Objects.requireNonNull(pdc, "pdc");
            return this;
        }

        public Builder components(@NotNull ItemComponentSnapshot components) {
            this.components = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder enchantments(@NotNull Map<String, Integer> enchantments) {
            this.enchantments = Objects.requireNonNull(enchantments, "enchantments");
            return this;
        }

        public Builder attributes(@NotNull Map<String, String> attributes) {
            this.attributes = Objects.requireNonNull(attributes, "attributes");
            return this;
        }

        public Builder rawNbt(@Nullable String rawNbt) {
            this.rawNbt = rawNbt;
            return this;
        }

        public ItemSnapshot build() {
            return new ItemSnapshot(
                    this.type,
                    this.amount,
                    this.meta,
                    this.pdc,
                    this.components,
                    this.enchantments,
                    this.attributes,
                    Optional.ofNullable(this.rawNbt)
            );
        }
    }
}
