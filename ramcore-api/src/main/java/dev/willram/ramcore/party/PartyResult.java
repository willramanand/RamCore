package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Result for party mutations.
 */
public record PartyResult<T>(boolean success, @Nullable T value, @NotNull String message) {

    @NotNull
    public static PartyResult<Void> ok() {
        return new PartyResult<>(true, null, "");
    }

    @NotNull
    public static <T> PartyResult<T> ok(@NotNull T value) {
        return new PartyResult<>(true, value, "");
    }

    @NotNull
    public static <T> PartyResult<T> failure(@NotNull String message) {
        return new PartyResult<>(false, null, message);
    }

    @NotNull
    public Optional<T> optional() {
        return Optional.ofNullable(this.value);
    }
}
