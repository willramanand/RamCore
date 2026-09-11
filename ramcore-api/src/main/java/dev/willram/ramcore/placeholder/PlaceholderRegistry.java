package dev.willram.ramcore.placeholder;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Holds {@link PlaceholderProvider}s and resolves placeholders without depending on PlaceholderAPI.
 *
 * <p>A placeholder key is {@code <id>_<params>}: the segment before the first underscore selects the
 * provider, the rest is the params. {@link #tagResolver(OfflinePlayer)} bridges the providers into
 * MiniMessage, so {@code <ramcore:'party_size'>} resolves through the {@code ramcore} provider.</p>
 *
 * <p>Stability: stable.</p>
 */
public final class PlaceholderRegistry {
    private final Map<String, PlaceholderProvider> providers = new LinkedHashMap<>();

    @NotNull
    public static PlaceholderRegistry create() {
        return new PlaceholderRegistry();
    }

    @NotNull
    public PlaceholderRegistry register(@NotNull PlaceholderProvider provider) {
        requireNonNull(provider, "provider");
        String id = provider.id();
        RamPreconditions.checkArgument(id != null && !id.isBlank() && !id.contains("_"),
                "placeholder provider id must be non-blank and contain no underscore",
                "Use a short id such as 'ramcore'.");
        RamPreconditions.checkArgument(!this.providers.containsKey(id), "placeholder provider already registered: " + id, "Use a unique id.");
        this.providers.put(id, provider);
        return this;
    }

    @NotNull
    public List<PlaceholderProvider> providers() {
        return List.copyOf(this.providers.values());
    }

    /**
     * Resolves a full placeholder key of the form {@code <id>_<params>}.
     *
     * @param player the player
     * @param key    the full key, for example {@code ramcore_party_size}
     * @return the resolved value, or empty
     */
    @NotNull
    public Optional<String> resolve(@NotNull OfflinePlayer player, @NotNull String key) {
        requireNonNull(player, "player");
        requireNonNull(key, "key");
        int underscore = key.indexOf('_');
        String id = underscore < 0 ? key : key.substring(0, underscore);
        String params = underscore < 0 ? "" : key.substring(underscore + 1);
        PlaceholderProvider provider = this.providers.get(id);
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(provider.resolve(player, params));
    }

    /**
     * A MiniMessage resolver bridging every provider. Each tag is named after a provider id and
     * takes the params as its first argument, for example {@code <ramcore:'party_size'>}.
     *
     * @param player the player to resolve for
     * @return the tag resolver
     */
    @NotNull
    public TagResolver tagResolver(@NotNull OfflinePlayer player) {
        requireNonNull(player, "player");
        List<TagResolver> resolvers = new ArrayList<>();
        for (PlaceholderProvider provider : this.providers.values()) {
            resolvers.add(TagResolver.resolver(provider.id(), (args, ctx) -> {
                String params = args.hasNext() ? args.pop().value() : "";
                String value = provider.resolve(player, params);
                return value == null ? null : Tag.inserting(Component.text(value));
            }));
        }
        return TagResolver.resolver(resolvers);
    }
}
