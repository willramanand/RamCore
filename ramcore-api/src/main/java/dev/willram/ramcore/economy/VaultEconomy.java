package dev.willram.ramcore.economy;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * An {@link Economy} backed by a Vault economy provider. Obtain one only through
 * {@link Economies#detect}, which guards against loading {@code net.milkbowl} classes when Vault is
 * absent. Runs on the main thread; callers choose the scheduler context.
 */
final class VaultEconomy implements Economy {
    private final net.milkbowl.vault.economy.Economy vault;

    VaultEconomy(@NotNull net.milkbowl.vault.economy.Economy vault) {
        this.vault = requireNonNull(vault, "vault");
    }

    @Override
    public double balance(@NotNull UUID player) {
        return this.vault.getBalance(offline(player));
    }

    @Override
    public boolean has(@NotNull UUID player, double amount) {
        return this.vault.has(offline(player), amount);
    }

    @NotNull
    @Override
    public EconomyResult withdraw(@NotNull UUID player, double amount) {
        return result(this.vault.withdrawPlayer(offline(player), amount));
    }

    @NotNull
    @Override
    public EconomyResult deposit(@NotNull UUID player, double amount) {
        return result(this.vault.depositPlayer(offline(player), amount));
    }

    @NotNull
    @Override
    public String format(double amount) {
        return this.vault.format(amount);
    }

    @NotNull
    @Override
    public String currencyName(boolean plural) {
        return plural ? this.vault.currencyNamePlural() : this.vault.currencyNameSingular();
    }

    private static OfflinePlayer offline(@NotNull UUID player) {
        return Bukkit.getOfflinePlayer(requireNonNull(player, "player"));
    }

    private static EconomyResult result(@NotNull EconomyResponse response) {
        if (response.transactionSuccess()) {
            return EconomyResult.success(response.balance);
        }
        return EconomyResult.failure(response.balance, response.errorMessage == null ? "transaction failed" : response.errorMessage);
    }
}
