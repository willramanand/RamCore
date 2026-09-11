package dev.willram.ramcore.message;

import dev.willram.ramcore.text.TextContext;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * MiniMessage-backed message catalog with optional prefix rendering and per-locale templates.
 *
 * <p>Templates are grouped by {@link Locale}. {@link Builder#message} and {@link Builder#messages}
 * populate the default locale ({@link Locale#US} unless {@link Builder#defaultLocale} changes it);
 * {@link Builder#locale} adds a translated set. Lookup for a locale follows a fallback chain and
 * never throws for a missing template: exact locale, then the language-only locale ({@code en_GB}
 * to {@code en}), then the default locale, then {@link MessageKey#defaultTemplate()}, then the key
 * id.</p>
 *
 * <p>{@link #send(Audience, MessageKey, TagResolver...)} resolves the audience locale through the
 * configured {@link LocaleResolver} (by default {@code Player.locale()} for players, the default
 * locale otherwise). With no locales registered every path collapses to the default locale, so the
 * behaviour is identical to a catalog that never knew about locales.</p>
 *
 * <p>Stability: stable. Folia-safe.</p>
 */
public final class MessageCatalog {
    private final MiniMessage miniMessage;
    private final String prefix;
    private final Locale defaultLocale;
    private final Map<Locale, Map<MessageKey, String>> byLocale;
    private final LocaleResolver localeResolver;

    private MessageCatalog(@NotNull MiniMessage miniMessage, @NotNull String prefix, @NotNull Locale defaultLocale,
                           @NotNull Map<Locale, Map<MessageKey, String>> byLocale, @NotNull LocaleResolver localeResolver) {
        this.miniMessage = requireNonNull(miniMessage, "miniMessage");
        this.prefix = requireNonNull(prefix, "prefix");
        this.defaultLocale = requireNonNull(defaultLocale, "defaultLocale");
        Map<Locale, Map<MessageKey, String>> copy = new LinkedHashMap<>();
        byLocale.forEach((locale, templates) -> copy.put(locale, Map.copyOf(templates)));
        this.byLocale = Map.copyOf(copy);
        this.localeResolver = requireNonNull(localeResolver, "localeResolver");
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public Locale defaultLocale() {
        return this.defaultLocale;
    }

    // ---- render (default locale) ----

    @NotNull
    public Component render(@NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        return render(this.defaultLocale, key, placeholders);
    }

    @NotNull
    public Component render(@NotNull MessageKey key, @NotNull TextContext context) {
        return render(key, requireNonNull(context, "context").resolver());
    }

    @NotNull
    public Component renderRaw(@NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        return renderRaw(this.defaultLocale, key, placeholders);
    }

    @NotNull
    public Component renderRaw(@NotNull MessageKey key, @NotNull TextContext context) {
        return renderRaw(key, requireNonNull(context, "context").resolver());
    }

    // ---- render (explicit locale) ----

    @NotNull
    public Component render(@NotNull Locale locale, @NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        String template = template(locale, key);
        TagResolver resolver = TagResolver.resolver(placeholders);
        if (this.prefix.isBlank()) {
            return this.miniMessage.deserialize(template, resolver);
        }
        return this.miniMessage.deserialize(this.prefix + template, resolver);
    }

    @NotNull
    public Component render(@NotNull Locale locale, @NotNull MessageKey key, @NotNull TextContext context) {
        return render(locale, key, requireNonNull(context, "context").resolver());
    }

    @NotNull
    public Component renderRaw(@NotNull Locale locale, @NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        return this.miniMessage.deserialize(template(locale, key), TagResolver.resolver(placeholders));
    }

    @NotNull
    public Component renderRaw(@NotNull Locale locale, @NotNull MessageKey key, @NotNull TextContext context) {
        return renderRaw(locale, key, requireNonNull(context, "context").resolver());
    }

    // ---- send (resolves the audience locale) ----

    public void send(@NotNull Audience audience, @NotNull MessageKey key, @NotNull TagResolver... placeholders) {
        requireNonNull(audience, "audience");
        audience.sendMessage(render(this.localeResolver.resolve(audience), key, placeholders));
    }

    public void send(@NotNull Audience audience, @NotNull MessageKey key, @NotNull TextContext context) {
        send(audience, key, requireNonNull(context, "context").resolver());
    }

    // ---- template lookup ----

    /**
     * The template for a key in the default locale (fallback to {@link MessageKey#defaultTemplate()}).
     *
     * @param key the key
     * @return the template
     */
    @NotNull
    public String template(@NotNull MessageKey key) {
        return template(this.defaultLocale, key);
    }

    /**
     * The template for a key in a locale, following the fallback chain.
     *
     * @param locale the locale
     * @param key    the key
     * @return the template; never null and never throws
     */
    @NotNull
    public String template(@NotNull Locale locale, @NotNull MessageKey key) {
        requireNonNull(locale, "locale");
        requireNonNull(key, "key");

        String exact = lookup(locale, key);
        if (exact != null) {
            return exact;
        }
        if (!locale.getCountry().isEmpty() || !locale.getVariant().isEmpty()) {
            String language = lookup(Locale.of(locale.getLanguage()), key);
            if (language != null) {
                return language;
            }
        }
        String fallback = lookup(this.defaultLocale, key);
        if (fallback != null) {
            return fallback;
        }
        String template = key.defaultTemplate();
        return template.isBlank() ? key.id() : template;
    }

    private String lookup(@NotNull Locale locale, @NotNull MessageKey key) {
        Map<MessageKey, String> templates = this.byLocale.get(locale);
        return templates == null ? null : templates.get(key);
    }

    /**
     * The configured template for a key in the default locale, if any (ignores fallbacks).
     *
     * @param key the key
     * @return the configured template
     */
    @NotNull
    public Optional<String> configuredTemplate(@NotNull MessageKey key) {
        return configuredTemplate(this.defaultLocale, key);
    }

    /**
     * The configured template for a key in a specific locale, if any (ignores fallbacks).
     *
     * @param locale the locale
     * @param key    the key
     * @return the configured template
     */
    @NotNull
    public Optional<String> configuredTemplate(@NotNull Locale locale, @NotNull MessageKey key) {
        requireNonNull(locale, "locale");
        return Optional.ofNullable(lookup(locale, requireNonNull(key, "key")));
    }

    @NotNull
    public String prefix() {
        return this.prefix;
    }

    public static final class Builder {
        private MiniMessage miniMessage = MiniMessage.miniMessage();
        private String prefix = "";
        private Locale defaultLocale = Locale.US;
        private final Map<MessageKey, String> defaultTemplates = new LinkedHashMap<>();
        private final Map<Locale, Map<MessageKey, String>> localeTemplates = new LinkedHashMap<>();
        private LocaleResolver localeResolver;

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
        public Builder defaultLocale(@NotNull Locale defaultLocale) {
            this.defaultLocale = requireNonNull(defaultLocale, "defaultLocale");
            return this;
        }

        @NotNull
        public Builder localeResolver(@NotNull LocaleResolver localeResolver) {
            this.localeResolver = requireNonNull(localeResolver, "localeResolver");
            return this;
        }

        /**
         * Adds a template to the default locale. Overrides {@link MessageKey#defaultTemplate()}.
         *
         * @param key      the key
         * @param template the MiniMessage template
         * @return this builder
         */
        @NotNull
        public Builder message(@NotNull MessageKey key, @NotNull String template) {
            this.defaultTemplates.put(requireNonNull(key, "key"), requireNonNull(template, "template"));
            return this;
        }

        @NotNull
        public Builder messages(@NotNull Map<MessageKey, String> templates) {
            this.defaultTemplates.putAll(requireNonNull(templates, "templates"));
            return this;
        }

        /**
         * Adds or extends a locale's template set.
         *
         * @param locale    the locale
         * @param templates its templates
         * @return this builder
         */
        @NotNull
        public Builder locale(@NotNull Locale locale, @NotNull Map<MessageKey, String> templates) {
            requireNonNull(locale, "locale");
            requireNonNull(templates, "templates");
            this.localeTemplates.computeIfAbsent(locale, ignored -> new LinkedHashMap<>()).putAll(templates);
            return this;
        }

        /**
         * Applies a loaded bundle: sets the default locale and merges every locale's templates.
         *
         * @param bundle a bundle from {@link MessageCatalogLoader}
         * @return this builder
         */
        @NotNull
        public Builder load(@NotNull MessageCatalogLoader.Bundle bundle) {
            requireNonNull(bundle, "bundle").applyTo(this);
            return this;
        }

        @NotNull
        public MessageCatalog build() {
            Map<Locale, Map<MessageKey, String>> merged = new LinkedHashMap<>();
            this.localeTemplates.forEach((locale, templates) -> merged.put(locale, new LinkedHashMap<>(templates)));
            // default-locale message()/messages() entries merge last so they win over a locale(defaultLocale, ..) set
            merged.computeIfAbsent(this.defaultLocale, ignored -> new LinkedHashMap<>()).putAll(this.defaultTemplates);
            LocaleResolver resolver = this.localeResolver != null ? this.localeResolver : LocaleResolver.byPlayerLocale(this.defaultLocale);
            return new MessageCatalog(this.miniMessage, this.prefix, this.defaultLocale, merged, resolver);
        }
    }
}
