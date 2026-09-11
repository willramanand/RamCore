package dev.willram.ramcore.item.nbt;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Objects;

/**
 * Stable Bukkit/Paper binary item serialization wrapper.
 */
public final class BukkitItemBinaryCodec {
    private static final String PREFIX = "ramcore-item-bytes:v1:";

    @NotNull
    public byte[] exportBytes(@NotNull ItemStack item) {
        return Objects.requireNonNull(item, "item").serializeAsBytes();
    }

    @NotNull
    public ItemStack importBytes(byte @NotNull [] bytes) {
        return ItemStack.deserializeBytes(Objects.requireNonNull(bytes, "bytes"));
    }

    @NotNull
    public String exportBase64(@NotNull ItemStack item) {
        return PREFIX + Base64.getEncoder().encodeToString(exportBytes(item));
    }

    @NotNull
    public ItemStack importBase64(@NotNull String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        String payload = encoded.startsWith(PREFIX) ? encoded.substring(PREFIX.length()) : encoded;
        return importBytes(Base64.getDecoder().decode(payload));
    }
}
