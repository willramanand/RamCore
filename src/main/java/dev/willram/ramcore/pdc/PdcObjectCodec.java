package dev.willram.ramcore.pdc;

import org.jetbrains.annotations.NotNull;

/**
 * Converts objects to and from a PDC-storable string representation.
 */
public interface PdcObjectCodec<T> {

    @NotNull
    String encode(@NotNull T value);

    @NotNull
    T decode(@NotNull String value);
}
