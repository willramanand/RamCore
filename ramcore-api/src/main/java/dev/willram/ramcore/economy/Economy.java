package dev.willram.ramcore.economy;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A minimal economy abstraction keyed by player {@link UUID}.
 *
 * <p>Stability: stable for this interface and {@link InMemoryEconomy}; the Vault-backed
 * implementation is experimental. Threading: implementations backed by another plugin (Vault) run
 * on the main thread and do not hop; the caller chooses the scheduler context. {@link InMemoryEconomy}
 * is thread-safe.</p>
 */
public interface Economy {

    /**
     * The player's balance, or {@code 0} when they have no account.
     *
     * @param player the player id
     * @return the balance
     */
    double balance(@NotNull UUID player);

    /**
     * Whether the player has at least the given amount.
     *
     * @param player the player id
     * @param amount the amount
     * @return true if affordable
     */
    boolean has(@NotNull UUID player, double amount);

    /**
     * Removes money from the player if they can afford it.
     *
     * @param player the player id
     * @param amount the amount (must be non-negative)
     * @return the result
     */
    @NotNull
    EconomyResult withdraw(@NotNull UUID player, double amount);

    /**
     * Adds money to the player.
     *
     * @param player the player id
     * @param amount the amount (must be non-negative)
     * @return the result
     */
    @NotNull
    EconomyResult deposit(@NotNull UUID player, double amount);

    /**
     * Formats an amount for display, including the currency name.
     *
     * @param amount the amount
     * @return the formatted string
     */
    @NotNull
    String format(double amount);

    /**
     * The currency name.
     *
     * @param plural whether to return the plural form
     * @return the currency name
     */
    @NotNull
    String currencyName(boolean plural);
}
