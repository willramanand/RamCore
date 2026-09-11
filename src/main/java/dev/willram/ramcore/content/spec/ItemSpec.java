package dev.willram.ramcore.content.spec;

import dev.willram.ramcore.content.ContentDeserializeException;
import org.bukkit.Material;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Pure, off-server description of an item. The {@code material} is validated against Bukkit's
 * {@link Material} enum at deserialize time; turning it into an {@code ItemStack} is the registrar's
 * job on the server.
 *
 * @param material     a valid {@link Material} name
 * @param name         the display name (MiniMessage), or null
 * @param lore         the lore lines (MiniMessage)
 * @param amount       the stack size (>= 1)
 * @param customModelData custom model data, or null
 * @param enchantments enchantment id to level
 */
public record ItemSpec(
        @NotNull String material,
        @Nullable String name,
        @NotNull List<String> lore,
        int amount,
        @Nullable Integer customModelData,
        @NotNull Map<String, Integer> enchantments
) {

    public ItemSpec {
        requireNonNull(material, "material");
        lore = List.copyOf(lore);
        enchantments = Map.copyOf(enchantments);
    }

    /**
     * Deserializes an item node. Validates the material key.
     *
     * @param node the node
     * @return the spec
     * @throws ContentDeserializeException on an unknown material or bad amount
     */
    @NotNull
    public static ItemSpec deserialize(@NotNull ConfigurationNode node) {
        String material = node.node("material").getString();
        if (material == null || material.isBlank()) {
            throw new ContentDeserializeException("item is missing 'material'");
        }
        try {
            Material.valueOf(material.trim().toUpperCase());
        } catch (IllegalArgumentException unknown) {
            throw new ContentDeserializeException("unknown material '" + material + "'");
        }

        int amount = node.node("amount").getInt(1);
        if (amount < 1) {
            throw new ContentDeserializeException("item amount must be >= 1");
        }

        String name = node.node("name").getString();
        List<String> lore = node.node("lore").childrenList().stream()
                .map(ConfigurationNode::getString)
                .filter(Objects::nonNull)
                .toList();

        Integer customModelData = node.node("custom-model-data").virtual() ? null : node.node("custom-model-data").getInt();

        Map<String, Integer> enchantments = new LinkedHashMap<>();
        node.node("enchantments").childrenMap().forEach((key, value) -> enchantments.put(String.valueOf(key), value.getInt()));

        return new ItemSpec(material.trim().toUpperCase(), name, lore, amount, customModelData, enchantments);
    }
}
