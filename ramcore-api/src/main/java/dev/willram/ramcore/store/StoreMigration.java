package dev.willram.ramcore.store;

import org.jetbrains.annotations.NotNull;

/**
 * Upgrades a value from an older schema version.
 *
 * @param <V> value type
 */
@FunctionalInterface
public interface StoreMigration<V> {

    /**
     * Migrates a value.
     *
     * @param value       the value as loaded
     * @param fromVersion the version it was written with
     * @return the migrated value (may be the same instance, mutated)
     */
    @NotNull
    V migrate(@NotNull V value, int fromVersion);
}
