package dev.willram.ramcore.text;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Shared MiniMessage renderer for messages, commands, scoreboards, menus, and configs.
 */
public final class TextFormatter {
    private final MiniMessage miniMessage;

    private TextFormatter(@NotNull MiniMessage miniMessage) {
        this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
    }

    @NotNull
    public static TextFormatter create(@NotNull MiniMessage miniMessage) {
        return new TextFormatter(miniMessage);
    }

    @NotNull
    public Component component(@NotNull String template) {
        return component(template, TextContext.empty());
    }

    @NotNull
    public Component component(@NotNull String template, @NotNull TextContext context) {
        return this.miniMessage.deserialize(Objects.requireNonNull(template, "template"), Objects.requireNonNull(context, "context").resolver());
    }

    @NotNull
    public Component component(@NotNull String template, @NotNull TagResolver... resolvers) {
        return this.miniMessage.deserialize(Objects.requireNonNull(template, "template"), TagResolver.resolver(resolvers));
    }

    @NotNull
    public Component item(@NotNull String template, @NotNull TextContext context) {
        return component(template, context).decoration(TextDecoration.ITALIC, false);
    }

    @NotNull
    public String plain(@NotNull String template) {
        return plain(template, TextContext.empty());
    }

    @NotNull
    public String plain(@NotNull String template, @NotNull TextContext context) {
        return PlainTextComponentSerializer.plainText().serialize(component(template, context));
    }

    @NotNull
    public List<Component> components(@NotNull Collection<String> templates, @NotNull TextContext context) {
        Objects.requireNonNull(templates, "templates");
        return templates.stream().map(template -> component(template, context)).toList();
    }

    @NotNull
    public List<String> plainLines(@NotNull Collection<String> templates, @NotNull TextContext context) {
        Objects.requireNonNull(templates, "templates");
        return templates.stream().map(template -> plain(template, context)).toList();
    }

    public void send(@NotNull Audience audience, @NotNull String template, @NotNull TextContext context) {
        Objects.requireNonNull(audience, "audience").sendMessage(component(template, context));
    }
}
