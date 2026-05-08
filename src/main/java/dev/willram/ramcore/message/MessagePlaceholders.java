package dev.willram.ramcore.message;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Helpers for MiniMessage placeholder resolvers.
 */
public final class MessagePlaceholders {

    @NotNull
    public static TagResolver parsed(@NotNull String name, @NotNull Object value) {
        return Placeholder.parsed(requireNonNull(name, "name"), String.valueOf(requireNonNull(value, "value")));
    }

    @NotNull
    public static TagResolver unparsed(@NotNull String name, @NotNull Object value) {
        return Placeholder.unparsed(requireNonNull(name, "name"), String.valueOf(requireNonNull(value, "value")));
    }

    @NotNull
    public static TagResolver component(@NotNull String name, @NotNull ComponentLike value) {
        return Placeholder.component(requireNonNull(name, "name"), requireNonNull(value, "value"));
    }

    @NotNull
    public static TagResolver parsedMap(@NotNull Map<String, ?> placeholders) {
        return placeholders.entrySet().stream()
                .map(entry -> parsed(entry.getKey(), entry.getValue()))
                .collect(TagResolver.toTagResolver());
    }

    private MessagePlaceholders() {
    }
}
