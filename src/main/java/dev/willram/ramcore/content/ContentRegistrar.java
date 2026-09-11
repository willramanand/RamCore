package dev.willram.ramcore.content;

import dev.willram.ramcore.content.spec.ItemSpec;
import dev.willram.ramcore.content.spec.RegionSpec;
import dev.willram.ramcore.content.spec.RewardEntrySpec;
import dev.willram.ramcore.content.spec.RewardPlanSpec;
import dev.willram.ramcore.region.RegionRule;
import dev.willram.ramcore.region.RuleRegion;
import dev.willram.ramcore.reward.RewardActionFactories;
import dev.willram.ramcore.reward.RewardEntry;
import dev.willram.ramcore.reward.RewardPlan;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Turns pure specs into live objects. Region and reward conversion is off-server and pure; item
 * conversion needs a running server for item meta.
 *
 * <p>Stability: experimental.</p>
 */
public final class ContentRegistrar {

    private ContentRegistrar() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Builds a {@link RuleRegion} from a spec. Pure; safe off-server.
     *
     * @param id   the region id
     * @param spec the spec
     * @return the region
     */
    @NotNull
    public static RuleRegion toRuleRegion(@NotNull ContentId id, @NotNull RegionSpec spec) {
        requireNonNull(id, "id");
        requireNonNull(spec, "spec");
        RuleRegion.Builder builder = RuleRegion.builder(id, spec.shape()).priority(spec.priority());
        for (RegionRule rule : spec.rules()) {
            builder.rule(rule);
        }
        return builder.build();
    }

    /**
     * Builds a {@link RewardPlan} from a spec, resolving each entry's action through the factories.
     * Pure apart from whatever the built actions capture.
     *
     * @param spec      the spec
     * @param factories the reward action factories
     * @return the plan
     */
    @NotNull
    public static RewardPlan toRewardPlan(@NotNull RewardPlanSpec spec, @NotNull RewardActionFactories factories) {
        requireNonNull(spec, "spec");
        requireNonNull(factories, "factories");
        RewardPlan.Builder builder = RewardPlan.builder().rolls(spec.rolls());
        for (RewardEntrySpec entry : spec.guaranteed()) {
            builder.guaranteed(RewardEntry.guaranteed(entry.id(), action(entry, factories)));
        }
        for (RewardEntrySpec entry : spec.weighted()) {
            builder.weighted(RewardEntry.weighted(entry.id(), action(entry, factories), entry.weight()));
        }
        return builder.build();
    }

    private static dev.willram.ramcore.reward.RewardAction action(@NotNull RewardEntrySpec entry, @NotNull RewardActionFactories factories) {
        return factories.get(entry.type())
                .orElseThrow(() -> new ContentDeserializeException("unknown reward type '" + entry.type() + "'"))
                .create(entry.params());
    }

    /**
     * Builds an {@link ItemStack} from a spec. Requires a running server (item meta).
     *
     * @param spec the spec
     * @return the item stack
     */
    @NotNull
    public static ItemStack toItemStack(@NotNull ItemSpec spec) {
        requireNonNull(spec, "spec");
        ItemStack item = new ItemStack(Material.valueOf(spec.material()), spec.amount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (spec.name() != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize(spec.name()));
            }
            if (!spec.lore().isEmpty()) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                spec.lore().forEach(line -> lore.add(MiniMessage.miniMessage().deserialize(line)));
                meta.lore(lore);
            }
            if (spec.customModelData() != null) {
                meta.setCustomModelData(spec.customModelData());
            }
            spec.enchantments().forEach((key, level) -> {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase()));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, level, true);
                }
            });
            item.setItemMeta(meta);
        }
        return item;
    }
}
