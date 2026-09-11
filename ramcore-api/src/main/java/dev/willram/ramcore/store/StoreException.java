package dev.willram.ramcore.store;

import org.jetbrains.annotations.NotNull;

/**
 * A backend failure (I/O, SQL, codec). The store promise completes exceptionally with this.
 */
public final class StoreException extends RuntimeException {

    public StoreException(@NotNull String message) {
        super(message);
    }

    public StoreException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
