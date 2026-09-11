package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link StatSource} that reads the {@code ramcore:stats} map (see {@link ItemStats}) from a
 * player's equipped armour and held items and contributes each entry as an additive modifier keyed
 * by slot ({@code item:helmet}, {@code item:mainhand}, ...).
 *
 * <p>Reads live equipment, so it must be consulted on the player's thread.</p>
 */
public final class ItemStatSource implements StatSource {

    @Override
    @NotNull
    public List<StatModifier> modifiers(@NotNull Player player) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return List.of();
        }
        List<StatModifier> out = new ArrayList<>();
        collect(out, equipment.getHelmet(), "helmet");
        collect(out, equipment.getChestplate(), "chestplate");
        collect(out, equipment.getLeggings(), "leggings");
        collect(out, equipment.getBoots(), "boots");
        collect(out, equipment.getItemInMainHand(), "mainhand");
        collect(out, equipment.getItemInOffHand(), "offhand");
        return out;
    }

    private static void collect(@NotNull List<StatModifier> out, ItemStack item, @NotNull String slot) {
        Map<ContentId, Double> stats = ItemStats.read(item);
        for (Map.Entry<ContentId, Double> entry : stats.entrySet()) {
            out.add(StatModifier.add(entry.getKey(), entry.getValue(), "item:" + slot));
        }
    }
}
