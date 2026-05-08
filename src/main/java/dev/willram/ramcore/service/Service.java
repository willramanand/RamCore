package dev.willram.ramcore.service;

import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle-aware unit managed by a {@link ServiceRegistry}.
 */
public interface Service {

    default void load(@NotNull ServiceContext context) {
    }

    default void enable(@NotNull ServiceContext context) {
    }

    default void disable(@NotNull ServiceContext context) {
    }
}
