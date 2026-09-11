package dev.willram.ramcore.data;

import com.google.gson.Gson;
import dev.willram.ramcore.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

/**
 * Gson-backed repository item serializer.
 */
public final class GsonDataSerializer<V extends DataItem> implements DataSerializer<V> {
    private final Gson gson;
    private final Class<V> type;

    private GsonDataSerializer(@NotNull Gson gson, @NotNull Class<V> type) {
        this.gson = Objects.requireNonNull(gson, "gson");
        this.type = Objects.requireNonNull(type, "type");
    }

    @NotNull
    public static <V extends DataItem> GsonDataSerializer<V> standard(@NotNull Class<V> type) {
        return new GsonDataSerializer<>(GsonProvider.standard(), type);
    }

    @NotNull
    public static <V extends DataItem> GsonDataSerializer<V> pretty(@NotNull Class<V> type) {
        return new GsonDataSerializer<>(GsonProvider.prettyPrinting(), type);
    }

    @Override
    public @NotNull V read(@NotNull Reader reader) {
        V item = this.gson.fromJson(reader, this.type);
        if (item == null) {
            throw new IllegalArgumentException("Repository file did not contain a " + this.type.getName());
        }
        return item;
    }

    @Override
    public void write(@NotNull Writer writer, @NotNull V item) {
        this.gson.toJson(item, this.type, writer);
    }
}
