package dev.willram.ramcore.reload;

import dev.willram.ramcore.resourcepack.Sha1;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces a stable content hash of a Configurate node so a reload can tell whether a definition
 * actually changed. Map keys are sorted so ordering differences do not register as changes.
 */
public final class ContentHashing {

    private ContentHashing() {
    }

    /**
     * A stable SHA-1 hex hash of a node's merged value.
     *
     * @param node the node
     * @return the hash
     */
    @NotNull
    public static String hash(@NotNull ConfigurationNode node) {
        String canonical = canonical(node.raw());
        return Sha1.hex(Sha1.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    @NotNull
    static String canonical(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), v));
            StringBuilder out = new StringBuilder("{");
            sorted.forEach((k, v) -> out.append(k).append('=').append(canonical(v)).append(';'));
            return out.append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder out = new StringBuilder("[");
            for (Object element : list) {
                out.append(canonical(element)).append(';');
            }
            return out.append(']').toString();
        }
        return value.getClass().getSimpleName() + ":" + value;
    }
}
