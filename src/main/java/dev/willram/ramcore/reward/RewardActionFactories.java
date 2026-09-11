package dev.willram.ramcore.reward;

import dev.willram.ramcore.content.ContentDeserializeException;
import dev.willram.ramcore.economy.Economy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A registry of {@link RewardActionFactory}s keyed by {@code type}. Used to validate reward entry
 * types during content loading and to build the actions afterwards.
 *
 * <p>{@link #standard(Economy)} registers {@code money}, {@code command}, {@code message} and
 * {@code permission-node-check}. {@code item} is intentionally left to consumers because it needs a
 * server to build an {@code ItemStack}. Stability: experimental.</p>
 */
public final class RewardActionFactories {
    private final Map<String, RewardActionFactory> factories = new LinkedHashMap<>();

    @NotNull
    public static RewardActionFactories create() {
        return new RewardActionFactories();
    }

    /**
     * A registry with the server-independent built-ins plus {@code money}.
     *
     * @param economy the economy for the money factory
     * @return the registry
     */
    @NotNull
    public static RewardActionFactories standard(@NotNull Economy economy) {
        requireNonNull(economy, "economy");
        RewardActionFactories registry = create();
        registry.register(factory("money", params -> RewardActions.money(economy, params.node("amount").getDouble())));
        registry.register(factory("command", params -> RewardActions.command(requireString(params, "command"))));
        registry.register(factory("message", params -> RewardActions.message(MiniMessage.miniMessage().deserialize(requireString(params, "message")))));
        registry.register(factory("permission-node-check", params -> RewardActions.permissionCheck(requireString(params, "node"))));
        return registry;
    }

    @NotNull
    public RewardActionFactories register(@NotNull RewardActionFactory factory) {
        requireNonNull(factory, "factory");
        this.factories.put(factory.type(), factory);
        return this;
    }

    public boolean has(@NotNull String type) {
        return this.factories.containsKey(requireNonNull(type, "type"));
    }

    @NotNull
    public Optional<RewardActionFactory> get(@NotNull String type) {
        return Optional.ofNullable(this.factories.get(requireNonNull(type, "type")));
    }

    @NotNull
    public Set<String> types() {
        return Set.copyOf(this.factories.keySet());
    }

    @NotNull
    private static RewardActionFactory factory(@NotNull String type, @NotNull ParamAction action) {
        return new RewardActionFactory() {
            @NotNull
            @Override
            public String type() {
                return type;
            }

            @NotNull
            @Override
            public RewardAction create(@NotNull ConfigurationNode params) {
                return action.create(params);
            }
        };
    }

    @NotNull
    private static String requireString(@NotNull ConfigurationNode params, @NotNull String key) {
        String value = params.node(key).getString();
        if (value == null || value.isBlank()) {
            throw new ContentDeserializeException("reward is missing '" + key + "'");
        }
        return value;
    }

    @FunctionalInterface
    private interface ParamAction {
        @NotNull
        RewardAction create(@NotNull ConfigurationNode params);
    }
}
