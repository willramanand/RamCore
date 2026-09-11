package dev.willram.ramcore.reward;

import dev.willram.ramcore.content.ContentDeserializeException;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a {@link RewardAction} from a config node for one reward {@code type}. Consumers register
 * their own factories in a {@link RewardActionFactories} registry alongside the built-ins.
 */
public interface RewardActionFactory {

    /**
     * The {@code type} key this factory handles, for example {@code money}.
     *
     * @return the type key
     */
    @NotNull
    String type();

    /**
     * Creates the action from its parameter node.
     *
     * @param params the reward entry's node
     * @return the action
     * @throws ContentDeserializeException when the params are invalid
     */
    @NotNull
    RewardAction create(@NotNull ConfigurationNode params) throws ContentDeserializeException;
}
