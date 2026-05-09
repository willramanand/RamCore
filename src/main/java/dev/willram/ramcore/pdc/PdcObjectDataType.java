package dev.willram.ramcore.pdc;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Stores one object as a string using a caller-provided codec.
 */
public class PdcObjectDataType<T> implements PersistentDataType<String, T> {
    private final Class<T> type;
    private final PdcObjectCodec<T> codec;

    public PdcObjectDataType(@NotNull Class<T> type, @NotNull PdcObjectCodec<T> codec) {
        this.type = requireNonNull(type, "type");
        this.codec = requireNonNull(codec, "codec");
    }

    @NotNull
    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @NotNull
    @Override
    public Class<T> getComplexType() {
        return this.type;
    }

    @NotNull
    @Override
    public String toPrimitive(@NotNull T complex, @NotNull PersistentDataAdapterContext context) {
        return this.codec.encode(complex);
    }

    @NotNull
    @Override
    public T fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        return this.codec.decode(primitive);
    }
}
