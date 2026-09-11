package dev.willram.ramcore.economy;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A thread-safe in-memory {@link Economy}. Balances never go negative. Useful as a default, for
 * tests, and for single-server setups with no economy plugin.
 */
public final class InMemoryEconomy implements Economy {
    private final ConcurrentHashMap<UUID, Double> balances = new ConcurrentHashMap<>();
    private final String singular;
    private final String plural;

    public InMemoryEconomy() {
        this("coin", "coins");
    }

    public InMemoryEconomy(@NotNull String singular, @NotNull String plural) {
        this.singular = requireNonNull(singular, "singular");
        this.plural = requireNonNull(plural, "plural");
    }

    @Override
    public double balance(@NotNull UUID player) {
        return this.balances.getOrDefault(requireNonNull(player, "player"), 0.0d);
    }

    @Override
    public boolean has(@NotNull UUID player, double amount) {
        return balance(player) >= amount;
    }

    @NotNull
    @Override
    public EconomyResult withdraw(@NotNull UUID player, double amount) {
        requireNonNull(player, "player");
        checkAmount(amount);
        boolean[] ok = {false};
        this.balances.compute(player, (id, current) -> {
            double balance = current == null ? 0.0d : current;
            if (balance >= amount) {
                ok[0] = true;
                return balance - amount;
            }
            return current;
        });
        double balance = balance(player);
        return ok[0] ? EconomyResult.success(balance) : EconomyResult.failure(balance, "insufficient funds");
    }

    @NotNull
    @Override
    public EconomyResult deposit(@NotNull UUID player, double amount) {
        requireNonNull(player, "player");
        checkAmount(amount);
        double updated = this.balances.merge(player, amount, Double::sum);
        return EconomyResult.success(updated);
    }

    @NotNull
    @Override
    public String format(double amount) {
        String rounded = String.format("%.2f", amount);
        return rounded + " " + currencyName(amount != 1.0d);
    }

    @NotNull
    @Override
    public String currencyName(boolean plural) {
        return plural ? this.plural : this.singular;
    }

    private static void checkAmount(double amount) {
        RamPreconditions.checkArgument(amount >= 0, "economy amount must not be negative", "Withdraw or deposit a non-negative amount.");
    }
}
