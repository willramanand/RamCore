package dev.willram.ramcore.economy;

import dev.willram.ramcore.integration.IntegrationRegistry;
import dev.willram.ramcore.integration.StandardIntegrations;
import dev.willram.ramcore.utils.RamLog;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Factory for {@link Economy} instances.
 */
public final class Economies {

    private Economies() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * A fresh in-memory economy.
     *
     * @return the economy
     */
    @NotNull
    public static Economy inMemory() {
        return new InMemoryEconomy();
    }

    /**
     * A Vault-backed economy, if Vault is available and a provider is registered.
     *
     * <p>{@code net.milkbowl} classes are only touched when the registry reports Vault available, so
     * this is safe to call when Vault is absent.</p>
     *
     * @param registry the integration registry (typically {@code Integrations.standard()})
     * @return the economy, or empty when Vault or a provider is missing
     */
    @NotNull
    public static Optional<Economy> detect(@NotNull IntegrationRegistry registry) {
        requireNonNull(registry, "registry");
        if (!registry.available(StandardIntegrations.VAULT.id())) {
            return Optional.empty();
        }
        return resolveVault();
    }

    private static Optional<Economy> resolveVault() {
        try {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> registration =
                    Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (registration == null) {
                return Optional.empty();
            }
            return Optional.of(new VaultEconomy(registration.getProvider()));
        } catch (Throwable error) {
            RamLog.warn("Vault is present but its economy provider could not be resolved", error);
            return Optional.empty();
        }
    }
}
