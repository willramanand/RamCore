package dev.willram.ramcore.economy;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * The result of an {@link Economy} transaction.
 *
 * @param success    whether the transaction happened
 * @param newBalance the balance after the transaction (the unchanged balance on failure)
 * @param message    a human-readable note, empty on success
 */
public record EconomyResult(boolean success, double newBalance, @NotNull String message) {

    public EconomyResult {
        requireNonNull(message, "message");
    }

    @NotNull
    public static EconomyResult success(double newBalance) {
        return new EconomyResult(true, newBalance, "");
    }

    @NotNull
    public static EconomyResult failure(double balance, @NotNull String message) {
        return new EconomyResult(false, balance, message);
    }
}
