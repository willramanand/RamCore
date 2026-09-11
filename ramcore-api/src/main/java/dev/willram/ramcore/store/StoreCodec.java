package dev.willram.ramcore.store;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.willram.ramcore.data.DataItem;
import dev.willram.ramcore.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Serialises a {@link StoredRecord} to and from a string for file and SQL backends.
 *
 * @param <V> value type
 */
public interface StoreCodec<V> {

    /**
     * Encodes a record, including its version.
     *
     * @param record the record
     * @return the encoded form
     */
    @NotNull
    String encode(@NotNull StoredRecord<V> record);

    /**
     * Decodes a record.
     *
     * @param encoded the encoded form
     * @return the record
     * @throws StoreException when the input cannot be decoded
     */
    @NotNull
    StoredRecord<V> decode(@NotNull String encoded);

    /**
     * JSON envelope codec for any Gson-serialisable type: {@code {"version": n, "data": {...}}}.
     *
     * @param type the value type
     * @param <V>  value type
     * @return the codec
     */
    @NotNull
    static <V> StoreCodec<V> gson(@NotNull Class<V> type) {
        return new GsonEnvelopeCodec<>(GsonProvider.prettyPrinting(), type);
    }

    /**
     * JSON envelope codec using a custom Gson instance.
     *
     * @param gson the Gson instance
     * @param type the value type
     * @param <V>  value type
     * @return the codec
     */
    @NotNull
    static <V> StoreCodec<V> gson(@NotNull Gson gson, @NotNull Class<V> type) {
        return new GsonEnvelopeCodec<>(gson, type);
    }

    /**
     * Raw JSON codec for {@link DataItem} subclasses. The version is the item's own
     * {@code dataVersion} field, so files written by {@code FileDataRepository} stay readable.
     *
     * @param type the item type
     * @param <V>  item type
     * @return the codec
     */
    @NotNull
    static <V extends DataItem> StoreCodec<V> dataItem(@NotNull Class<V> type) {
        return new DataItemCodec<>(GsonProvider.prettyPrinting(), type);
    }

    final class GsonEnvelopeCodec<V> implements StoreCodec<V> {
        private static final String VERSION = "version";
        private static final String DATA = "data";

        private final Gson gson;
        private final Class<V> type;

        GsonEnvelopeCodec(Gson gson, Class<V> type) {
            this.gson = Objects.requireNonNull(gson, "gson");
            this.type = Objects.requireNonNull(type, "type");
        }

        @NotNull
        @Override
        public String encode(@NotNull StoredRecord<V> record) {
            JsonObject envelope = new JsonObject();
            envelope.addProperty(VERSION, record.dataVersion());
            envelope.add(DATA, this.gson.toJsonTree(record.value(), this.type));
            return this.gson.toJson(envelope);
        }

        @NotNull
        @Override
        public StoredRecord<V> decode(@NotNull String encoded) {
            try {
                JsonElement element = JsonParser.parseString(encoded);
                if (!element.isJsonObject() || !element.getAsJsonObject().has(DATA)) {
                    throw new StoreException("expected a {version, data} envelope for " + this.type.getSimpleName());
                }
                JsonObject envelope = element.getAsJsonObject();
                int version = envelope.has(VERSION) ? envelope.get(VERSION).getAsInt() : StoredRecord.INITIAL_VERSION;
                V value = this.gson.fromJson(envelope.get(DATA), this.type);
                if (value == null) {
                    throw new StoreException("envelope data decoded to null for " + this.type.getSimpleName());
                }
                return new StoredRecord<>(version, value);
            } catch (StoreException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new StoreException("failed to decode " + this.type.getSimpleName(), e);
            }
        }
    }

    final class DataItemCodec<V extends DataItem> implements StoreCodec<V> {
        private final Gson gson;
        private final Class<V> type;

        DataItemCodec(Gson gson, Class<V> type) {
            this.gson = Objects.requireNonNull(gson, "gson");
            this.type = Objects.requireNonNull(type, "type");
        }

        @NotNull
        @Override
        public String encode(@NotNull StoredRecord<V> record) {
            record.value().dataVersion(record.dataVersion());
            return this.gson.toJson(record.value(), this.type);
        }

        @NotNull
        @Override
        public StoredRecord<V> decode(@NotNull String encoded) {
            try {
                V item = this.gson.fromJson(encoded, this.type);
                if (item == null) {
                    throw new StoreException("decoded null " + this.type.getSimpleName());
                }
                int version = Math.max(StoredRecord.INITIAL_VERSION, item.dataVersion());
                item.dataVersion(version);
                return new StoredRecord<>(version, item);
            } catch (StoreException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new StoreException("failed to decode " + this.type.getSimpleName(), e);
            }
        }
    }
}
