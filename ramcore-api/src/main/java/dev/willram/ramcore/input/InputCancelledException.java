package dev.willram.ramcore.input;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Completes a {@link PlayerInput} promise exceptionally when the input was not collected.
 */
public final class InputCancelledException extends RuntimeException {

    /**
     * Why an input request ended without a value.
     */
    public enum Reason {
        /** The player typed the cancel word. */
        CANCELLED,
        /** The request timed out. */
        TIMEOUT,
        /** The player left the server. */
        QUIT,
        /** Validation or parsing failed and no retries remained. */
        EXHAUSTED,
        /** A newer request for the same player replaced this one. */
        SUPERSEDED,
        /** The player was offline when the request tried to open. */
        OFFLINE
    }

    private final Reason reason;

    public InputCancelledException(@NotNull Reason reason) {
        super(requireNonNull(reason, "reason").name());
        this.reason = reason;
    }

    @NotNull
    public Reason reason() {
        return this.reason;
    }
}
