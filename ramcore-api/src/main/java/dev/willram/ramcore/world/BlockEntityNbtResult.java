package dev.willram.ramcore.world;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Result wrapper for optional raw block-entity SNBT support.
 */
public record BlockEntityNbtResult<T>(boolean supported, @NotNull Optional<T> value, @NotNull String message) {

    @NotNull
    public static <T> BlockEntityNbtResult<T> supported(@NotNull T value) {
        return new BlockEntityNbtResult<>(true, Optional.of(Objects.requireNonNull(value, "value")), "OK");
    }

    @NotNull
    public static <T> BlockEntityNbtResult<T> unsupported(@NotNull String message) {
        return new BlockEntityNbtResult<>(false, Optional.empty(), message);
    }

    public BlockEntityNbtResult {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(message, "message");
    }
}
