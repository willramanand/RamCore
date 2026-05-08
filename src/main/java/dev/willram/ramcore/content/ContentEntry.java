package dev.willram.ramcore.content;

import org.jetbrains.annotations.NotNull;

/**
 * Registered content plus owner metadata.
 */
public record ContentEntry<T>(
        @NotNull ContentKey<T> key,
        @NotNull String owner,
        @NotNull T value
) {
}
