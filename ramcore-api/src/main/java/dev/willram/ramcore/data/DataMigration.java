package dev.willram.ramcore.data;

import org.jetbrains.annotations.NotNull;

/**
 * Migrates one repository item from an older data version.
 */
@FunctionalInterface
public interface DataMigration<V extends DataItem> {

    @NotNull
    V migrate(@NotNull V item, int fromVersion);
}
