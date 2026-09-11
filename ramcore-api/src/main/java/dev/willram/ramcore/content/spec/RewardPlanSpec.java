package dev.willram.ramcore.content.spec;

import dev.willram.ramcore.content.ContentDeserializeException;
import dev.willram.ramcore.reward.RewardActionFactories;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A pure description of a reward plan: its roll count and its entries, split into guaranteed and
 * weighted. Each entry's {@code type} is validated against the supplied {@link RewardActionFactories}
 * so an unknown reward type surfaces as a load error.
 *
 * @param rolls      weighted rolls to make
 * @param guaranteed guaranteed entries
 * @param weighted   weighted entries
 */
public record RewardPlanSpec(int rolls, @NotNull List<RewardEntrySpec> guaranteed, @NotNull List<RewardEntrySpec> weighted) {

    public RewardPlanSpec {
        guaranteed = List.copyOf(guaranteed);
        weighted = List.copyOf(weighted);
    }

    /**
     * Deserializes a reward plan node.
     *
     * @param node      the node (with {@code rolls} and an {@code entries} list)
     * @param factories the known reward action types
     * @return the spec
     * @throws ContentDeserializeException on an unknown type or malformed entry
     */
    @NotNull
    public static RewardPlanSpec deserialize(@NotNull ConfigurationNode node, @NotNull RewardActionFactories factories) {
        requireNonNull(node, "node");
        requireNonNull(factories, "factories");

        int rolls = node.node("rolls").getInt(1);
        List<RewardEntrySpec> guaranteed = new ArrayList<>();
        List<RewardEntrySpec> weighted = new ArrayList<>();

        List<? extends ConfigurationNode> entries = node.node("entries").childrenList();
        for (int i = 0; i < entries.size(); i++) {
            ConfigurationNode entry = entries.get(i);
            String type = entry.node("type").getString();
            if (type == null || type.isBlank()) {
                throw new ContentDeserializeException("reward entry [" + i + "] is missing 'type'");
            }
            if (!factories.has(type)) {
                throw new ContentDeserializeException("unknown reward type '" + type + "' (known: " + factories.types() + ")");
            }
            String id = entry.node("id").getString(type + "-" + i);
            boolean guaranteedEntry = entry.node("guaranteed").getBoolean(false);
            double weight = entry.node("weight").getDouble(1.0);
            RewardEntrySpec spec = new RewardEntrySpec(id, type, guaranteedEntry, weight, entry);
            (guaranteedEntry ? guaranteed : weighted).add(spec);
        }

        return new RewardPlanSpec(rolls, guaranteed, weighted);
    }
}
