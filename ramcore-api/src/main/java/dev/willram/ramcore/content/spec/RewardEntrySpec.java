package dev.willram.ramcore.content.spec;

import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A pure description of one reward entry: its id, action {@code type}, whether it is guaranteed, its
 * weight, and the node its factory reads.
 *
 * @param id         the entry id
 * @param type       the reward action type (validated against a factory registry)
 * @param guaranteed whether the entry always applies (else it is a weighted roll)
 * @param weight     the roll weight (used only when not guaranteed)
 * @param params     the node passed to the {@code RewardActionFactory}
 */
public record RewardEntrySpec(
        @NotNull String id,
        @NotNull String type,
        boolean guaranteed,
        double weight,
        @NotNull ConfigurationNode params
) {

    public RewardEntrySpec {
        requireNonNull(id, "id");
        requireNonNull(type, "type");
        requireNonNull(params, "params");
    }
}
