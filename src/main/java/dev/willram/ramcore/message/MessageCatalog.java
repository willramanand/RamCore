package dev.willram.ramcore.message;

import dev.willram.ramcore.text.TextContext;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * MiniMessage-backed message catalog with optional prefix rendering.
 */
public final class MessageCatalog {
    private final MiniMessage miniMessage;
    private final String prefix;
    private final Map<MessageKey, String> templates;

    private MessageCatalog(@NotNull MiniMessage miniMessage, @NotNull String prefix, @NotNull Map<MessageKey, String> templates) {
        this.miniMessage = requireNonNull(miniMessage, "miniMessage");
        this.prefix = requireNonNull(prefix, "prefix");
        this.templates = Map.copyOf(templates);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public Component render(@NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        String template = template(key);
        TagResolver resolver = TagResolver.resolver(placeholders);
        if (this.prefix.isBlank()) {
            return this.miniMessage.deserialize(template, resolver);
        }

        return this.miniMessage.deserialize(this.prefix + template, resolver);
    }

    @NotNull
    public Component render(@NotNull MessageKey key, @NotNull TextContext context) {
        return render(key, requireNonNull(context, "context").resolver());
    }

    @NotNull
    public Component renderRaw(@NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        return this.miniMessage.deserialize(template(key), TagResolver.resolver(placeholders));
    }

    @NotNull
    public Component renderRaw(@NotNull MessageKey key, @NotNull TextContext context) {
        return renderRaw(key, requireNonNull(context, "context").resolver());
    }

    public void send(@NotNull Audience audience, @NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        requireNonNull(audience, "audience").sendMessage(render(key, placeholders));
    }

    public void send(@NotNull Audience audience, @NotNull MessageKey key, @NotNull TextContext context) {
        requireNonNull(audience, "audience").sendMessage(render(key, context));
    }

    @NotNull
    public Optional<String> configuredTemplate(@NotNull MessageKey key) {
        return Optional.ofNullable(this.templates.get(requireNonNull(key, "key")));
    }

    @NotNull
    public String template(@NotNull MessageKey key) {
        requireNonNull(key, "key");
        return this.templates.getOrDefault(key, key.defaultTemplate());
    }

    @NotNull
    public String prefix() {
        return this.prefix;
    }

    public static final class Builder {
        private MiniMessage miniMessage = MiniMessage.miniMessage();
        private String prefix = "";
        private final Map<MessageKey, String> templates = new LinkedHashMap<>();

        @NotNull
        public Builder miniMessage(@NotNull MiniMessage miniMessage) {
            this.miniMessage = requireNonNull(miniMessage, "miniMessage");
            return this;
        }

        @NotNull
        public Builder prefix(@NotNull String prefix) {
            this.prefix = requireNonNull(prefix, "prefix");
            return this;
        }

        @NotNull
        public Builder message(@NotNull MessageKey key, @NotNull String template) {
            this.templates.put(requireNonNull(key, "key"), requireNonNull(template, "template"));
            return this;
        }

        @NotNull
        public Builder messages(@NotNull Map<MessageKey, String> templates) {
            this.templates.putAll(requireNonNull(templates, "templates"));
            return this;
        }

        @NotNull
        public MessageCatalog build() {
            return new MessageCatalog(this.miniMessage, this.prefix, this.templates);
        }
    }
}
