package dev.willram.ramcore.item.nbt;

import dev.willram.ramcore.pdc.PdcEditor;
import dev.willram.ramcore.pdc.PdcKey;
import dev.willram.ramcore.pdc.PdcView;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Persistent custom item identity backed by item meta PDC.
 */
public final class CustomItemIdentityStore {
    private static final PdcKey<String, String> ID = PdcKey.of(new NamespacedKey("ramcore", "custom_item_id"), PersistentDataType.STRING);
    private static final PdcKey<Integer, Integer> VERSION = PdcKey.of(new NamespacedKey("ramcore", "custom_item_version"), PersistentDataType.INTEGER);

    @NotNull
    public static ItemStack apply(@NotNull ItemStack item, @NotNull CustomItemIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        ItemStack copy = Objects.requireNonNull(item, "item").clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            PdcEditor.of(meta.getPersistentDataContainer())
                    .set(ID, identity.key().toString())
                    .set(VERSION, identity.version());
            copy.setItemMeta(meta);
        }
        return copy;
    }

    @NotNull
    public static Optional<CustomItemIdentity> read(@NotNull ItemStack item) {
        ItemMeta meta = Objects.requireNonNull(item, "item").getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PdcView view = PdcView.of(meta.getPersistentDataContainer());
        return view.get(ID).map(id -> new CustomItemIdentity(NamespacedKey.fromString(id), view.getOrDefault(VERSION)));
    }

    public static boolean matches(@NotNull ItemStack item, @NotNull CustomItemIdentity identity) {
        return read(item).filter(identity::equals).isPresent();
    }

    private CustomItemIdentityStore() {
    }
}
