package dev.willram.ramcore.pdc;

import com.google.gson.Gson;
import dev.willram.ramcore.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Stores one object as JSON in a string PDC value.
 */
public final class PdcJsonDataType<T> extends PdcObjectDataType<T> {

    public PdcJsonDataType(@NotNull Class<T> type) {
        this(type, GsonProvider.standard());
    }

    public PdcJsonDataType(@NotNull Class<T> type, @NotNull Gson gson) {
        super(type, new JsonCodec<>(type, gson));
    }

    private record JsonCodec<T>(@NotNull Class<T> type, @NotNull Gson gson) implements PdcObjectCodec<T> {
        private JsonCodec {
            requireNonNull(type, "type");
            requireNonNull(gson, "gson");
        }

        @NotNull
        @Override
        public String encode(@NotNull T value) {
            return this.gson.toJson(requireNonNull(value, "value"));
        }

        @NotNull
        @Override
        public T decode(@NotNull String value) {
            return requireNonNull(this.gson.fromJson(value, this.type), "decoded value");
        }
    }
}
