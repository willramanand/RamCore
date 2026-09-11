package dev.willram.ramcore.placeholder;

import dev.willram.ramcore.integration.IntegrationRegistry;
import dev.willram.ramcore.integration.StandardIntegrations;
import dev.willram.ramcore.utils.RamLog;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Registers a {@link PlaceholderRegistry}'s providers with PlaceholderAPI, one
 * {@code PlaceholderExpansion} per provider id.
 *
 * <p>{@code me.clip} classes are only touched when the integration registry reports PlaceholderAPI
 * available, so {@link #register} is safe to call when PAPI is absent (it returns 0). Stability:
 * experimental (the bridge); the abstractions are stable.</p>
 */
public final class PlaceholderApiBridge {

    private PlaceholderApiBridge() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Registers each provider as a PlaceholderAPI expansion when PAPI is present.
     *
     * @param integrations the integration registry
     * @param placeholders the provider registry
     * @param author       the expansion author
     * @param version      the expansion version
     * @return the number of expansions registered, or 0 when PAPI is absent
     */
    public static int register(@NotNull IntegrationRegistry integrations, @NotNull PlaceholderRegistry placeholders,
                               @NotNull String author, @NotNull String version) {
        requireNonNull(integrations, "integrations");
        requireNonNull(placeholders, "placeholders");
        requireNonNull(author, "author");
        requireNonNull(version, "version");
        if (!integrations.available(StandardIntegrations.PLACEHOLDER_API.id())) {
            return 0;
        }
        return registerExpansions(placeholders, author, version);
    }

    private static int registerExpansions(@NotNull PlaceholderRegistry placeholders, @NotNull String author, @NotNull String version) {
        int registered = 0;
        for (PlaceholderProvider provider : placeholders.providers()) {
            try {
                if (new ProviderExpansion(provider, author, version).register()) {
                    registered++;
                }
            } catch (Throwable error) {
                RamLog.warn("failed to register PlaceholderAPI expansion for " + provider.id(), error);
            }
        }
        return registered;
    }

    private static final class ProviderExpansion extends PlaceholderExpansion {
        private final PlaceholderProvider provider;
        private final String author;
        private final String version;

        private ProviderExpansion(PlaceholderProvider provider, String author, String version) {
            this.provider = provider;
            this.author = author;
            this.version = version;
        }

        @NotNull
        @Override
        public String getIdentifier() {
            return this.provider.id();
        }

        @NotNull
        @Override
        public String getAuthor() {
            return this.author;
        }

        @NotNull
        @Override
        public String getVersion() {
            return this.version;
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Nullable
        @Override
        public String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
            if (player == null) {
                return null;
            }
            return this.provider.resolve(player, params);
        }
    }
}
