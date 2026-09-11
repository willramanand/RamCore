package dev.willram.ramcore.message;

import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Picks the {@link Locale} a message is rendered in for a given audience.
 *
 * <p>Folia-safe: {@link Player#locale()} is a cached field, safe to read off the region thread.</p>
 */
@FunctionalInterface
public interface LocaleResolver {

    /**
     * The locale to render for this audience.
     *
     * @param audience the recipient
     * @return the locale; never null
     */
    @NotNull
    Locale resolve(@NotNull Audience audience);

    /**
     * Resolves {@link Player#locale()} for a player audience and the default locale otherwise.
     *
     * @param defaultLocale the fallback for non-player audiences
     * @return the resolver
     */
    @NotNull
    static LocaleResolver byPlayerLocale(@NotNull Locale defaultLocale) {
        requireNonNull(defaultLocale, "defaultLocale");
        return audience -> audience instanceof Player player ? player.locale() : defaultLocale;
    }

    /**
     * Always resolves the given locale.
     *
     * @param locale the fixed locale
     * @return the resolver
     */
    @NotNull
    static LocaleResolver fixed(@NotNull Locale locale) {
        requireNonNull(locale, "locale");
        return audience -> locale;
    }
}
