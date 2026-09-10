package dev.willram.ramcore.store;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A value plus the schema version it was written with. Backends persist the version next to the
 * value so {@link StoreMigrations} can upgrade old entries on load.
 *
 * @param dataVersion schema version, at least 1
 * @param value       the value
 * @param <V>         value type
 */
public record StoredRecord<V>(int dataVersion, @NotNull V value) {

    public static final int INITIAL_VERSION = 1;

    public StoredRecord {
        if (dataVersion < INITIAL_VERSION) {
            throw new IllegalArgumentException("dataVersion must be >= " + INITIAL_VERSION + ": " + dataVersion);
        }
        Objects.requireNonNull(value, "value");
    }

    /**
     * Wraps a value at the initial version.
     *
     * @param value the value
     * @param <V>   value type
     * @return the record
     */
    @NotNull
    public static <V> StoredRecord<V> of(@NotNull V value) {
        return new StoredRecord<>(INITIAL_VERSION, value);
    }

    /**
     * Wraps a value at the given version.
     *
     * @param dataVersion the version
     * @param value       the value
     * @param <V>         value type
     * @return the record
     */
    @NotNull
    public static <V> StoredRecord<V> of(int dataVersion, @NotNull V value) {
        return new StoredRecord<>(dataVersion, value);
    }
}
