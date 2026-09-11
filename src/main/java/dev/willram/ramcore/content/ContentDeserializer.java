package dev.willram.ramcore.content;

import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

/**
 * Turns a merged {@link ContentDefinition} node into a pure spec object.
 *
 * @param <T> the spec type
 */
@FunctionalInterface
public interface ContentDeserializer<T> {

    /**
     * Deserializes one entry's node.
     *
     * @param node the merged node
     * @return the spec
     * @throws ContentDeserializeException when the node is invalid
     */
    @NotNull
    T deserialize(@NotNull ConfigurationNode node) throws ContentDeserializeException;
}
