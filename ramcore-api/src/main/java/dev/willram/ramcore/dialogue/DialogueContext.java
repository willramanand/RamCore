package dev.willram.ramcore.dialogue;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * The context of one dialogue interaction: the player and free-form metadata that conditions and
 * actions can read.
 *
 * @param player   the player in the dialogue
 * @param metadata free-form context data
 */
public record DialogueContext(@NotNull Player player, @NotNull Map<String, Object> metadata) {

    public DialogueContext {
        requireNonNull(player, "player");
        metadata = Map.copyOf(metadata);
    }

    /** A context with no metadata. */
    @NotNull
    public static DialogueContext of(@NotNull Player player) {
        return new DialogueContext(player, Map.of());
    }

    /** A copy of this context with the given metadata. */
    @NotNull
    public DialogueContext withMetadata(@NotNull Map<String, Object> metadata) {
        return new DialogueContext(this.player, metadata);
    }
}
