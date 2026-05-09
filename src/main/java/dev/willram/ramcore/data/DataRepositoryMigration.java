package dev.willram.ramcore.data;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Versioned data migration step.
 */
public record DataRepositoryMigration<V extends DataItem>(
        int targetVersion,
        @NotNull DataMigration<V> migration
) {

    public DataRepositoryMigration {
        if (targetVersion < 1) {
            throw new IllegalArgumentException("targetVersion must be >= 1");
        }
        Objects.requireNonNull(migration, "migration");
    }

    @NotNull
    public V apply(@NotNull V item) {
        return this.migration.migrate(item, item.dataVersion());
    }
}
