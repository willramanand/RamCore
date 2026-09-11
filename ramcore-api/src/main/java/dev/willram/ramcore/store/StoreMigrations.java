package dev.willram.ramcore.store;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Ordered schema migrations for a store. Each step declares the version it produces; on load,
 * every step whose target is above the record's version runs in ascending order, and the record
 * is rewritten at the highest version.
 *
 * <p>Immutable; {@link #to(int, StoreMigration)} returns a new chain.</p>
 *
 * @param <V> value type
 */
public final class StoreMigrations<V> {
    private static final StoreMigrations<?> NONE = new StoreMigrations<>(List.of());

    private final List<Step<V>> steps;

    private StoreMigrations(List<Step<V>> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * No migrations: every record is at {@link StoredRecord#INITIAL_VERSION}.
     *
     * @param <V> value type
     * @return the empty chain
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <V> StoreMigrations<V> none() {
        return (StoreMigrations<V>) NONE;
    }

    /**
     * Starts an empty chain to append steps to: {@code StoreMigrations.<Profile>start().to(2, ..)}.
     *
     * @param <V> value type
     * @return the empty chain
     */
    @NotNull
    public static <V> StoreMigrations<V> start() {
        return none();
    }

    /**
     * Appends a step.
     *
     * @param targetVersion version the step produces; must be higher than every existing step
     * @param migration     the step
     * @return a new chain
     */
    @NotNull
    public StoreMigrations<V> to(int targetVersion, @NotNull StoreMigration<V> migration) {
        Objects.requireNonNull(migration, "migration");
        RamPreconditions.checkArgument(targetVersion > StoredRecord.INITIAL_VERSION,
                "migration target version must be > " + StoredRecord.INITIAL_VERSION + ": " + targetVersion,
                "Version 1 is the initial schema; the first migration targets version 2.");
        RamPreconditions.checkArgument(targetVersion > currentVersion(),
                "migration target version " + targetVersion + " is not above the current version " + currentVersion(),
                "Register migrations in ascending version order.");
        List<Step<V>> next = new ArrayList<>(this.steps);
        next.add(new Step<>(targetVersion, migration));
        next.sort(Comparator.comparingInt(Step::targetVersion));
        return new StoreMigrations<>(next);
    }

    /**
     * The version new records are written with: the highest migration target, or 1.
     *
     * @return current schema version
     */
    public int currentVersion() {
        return this.steps.isEmpty() ? StoredRecord.INITIAL_VERSION : this.steps.getLast().targetVersion();
    }

    /**
     * Whether a record at the given version needs migrating.
     *
     * @param dataVersion the record's version
     * @return true if any step applies
     */
    public boolean needsMigration(int dataVersion) {
        return dataVersion < currentVersion();
    }

    /**
     * Applies every step above the record's version, in order.
     *
     * @param record the record as loaded
     * @return the migrated record, or the same instance when nothing applied
     */
    @NotNull
    public StoredRecord<V> apply(@NotNull StoredRecord<V> record) {
        Objects.requireNonNull(record, "record");
        if (!needsMigration(record.dataVersion())) {
            return record;
        }
        V value = record.value();
        int version = record.dataVersion();
        for (Step<V> step : this.steps) {
            if (version < step.targetVersion()) {
                value = Objects.requireNonNull(step.migration().migrate(value, version), "migration returned null");
                version = step.targetVersion();
            }
        }
        return new StoredRecord<>(version, value);
    }

    private record Step<V>(int targetVersion, StoreMigration<V> migration) {
    }
}
