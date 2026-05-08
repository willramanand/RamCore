package dev.willram.ramcore.service;

import org.jetbrains.annotations.NotNull;

/**
 * Mutable registration returned while composing services.
 */
public interface ServiceRegistration<T> {

    @NotNull
    ServiceRegistration<T> dependsOn(@NotNull ServiceKey<?> dependency);

    @NotNull
    ServiceKey<T> key();

    @NotNull
    T service();
}
