package dev.willram.ramcore.content;

import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * One loaded content entry: its id, type (the directory name), optional parent, the fully merged
 * configuration node (parent fields composed in), and where it came from.
 *
 * @param id     the content id
 * @param type   the content type, i.e. the directory name (items, loot, ...)
 * @param parent the parent id this entry extends, or null
 * @param node   the merged node, ready to deserialize into a spec
 * @param source where the entry was defined
 */
public record ContentDefinition(
        @NotNull ContentId id,
        @NotNull String type,
        @Nullable ContentId parent,
        @NotNull ConfigurationNode node,
        @NotNull SourceRef source
) {

    public ContentDefinition {
        requireNonNull(id, "id");
        requireNonNull(type, "type");
        requireNonNull(node, "node");
        requireNonNull(source, "source");
    }
}
