package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.pdc.PDCs;
import dev.willram.ramcore.pdc.PdcKey;
import dev.willram.ramcore.pdc.datatypes.collections.MapDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Reads and writes the {@code ramcore:stats} persistent-data map on items: a map of stat id
 * ({@code namespace:value}) to additive amount. Written by {@code ItemStackBuilder.stat(..)} and
 * read by {@link ItemStatSource}.
 */
public final class ItemStats {

    private static final PersistentDataType<PersistentDataContainer, HashMap<String, Double>> MAP_TYPE =
            new MapDataType<>(HashMap::new, PersistentDataType.STRING, PersistentDataType.DOUBLE);

    /** The typed key for the {@code ramcore:stats} item map. */
    public static final PdcKey<PersistentDataContainer, HashMap<String, Double>> KEY =
            PdcKey.of("ramcore", "stats", MAP_TYPE);

    private ItemStats() {
    }

    /**
     * The stat amounts stored on an item's meta, keyed by stat id. Entries whose key is not a valid
     * {@link ContentId} are skipped.
     *
     * @param meta the item meta
     * @return the stat amounts (possibly empty)
     */
    @NotNull
    public static Map<ContentId, Double> read(@NotNull ItemMeta meta) {
        requireNonNull(meta, "meta");
        Map<ContentId, Double> out = new LinkedHashMap<>();
        Map<String, Double> raw = PDCs.get(meta, KEY).orElse(null);
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            try {
                out.put(ContentId.parse(entry.getKey()), entry.getValue());
            } catch (RuntimeException ignored) {
                // skip malformed stat id
            }
        }
        return out;
    }

    /**
     * The stat amounts stored on an item, keyed by stat id.
     *
     * @param item the item (may be {@code null} or metaless → empty)
     * @return the stat amounts (possibly empty)
     */
    @NotNull
    public static Map<ContentId, Double> read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Map.of();
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null ? Map.of() : read(meta);
    }

    /**
     * Sets a stat amount on an item's meta, replacing any existing amount for that stat.
     *
     * @param meta   the item meta
     * @param id     the stat id
     * @param amount the additive amount
     */
    public static void put(@NotNull ItemMeta meta, @NotNull ContentId id, double amount) {
        requireNonNull(meta, "meta");
        requireNonNull(id, "id");
        HashMap<String, Double> map = PDCs.get(meta, KEY).orElseGet(HashMap::new);
        map.put(id.toString(), amount);
        PDCs.set(meta, KEY, map);
    }
}
