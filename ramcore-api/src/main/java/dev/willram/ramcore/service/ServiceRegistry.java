package dev.willram.ramcore.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registers services, resolves dependencies, and controls lifecycle order.
 */
public interface ServiceRegistry extends AutoCloseable {

    @NotNull
    static ServiceRegistry create(@NotNull ServiceContext context) {
        return new SimpleServiceRegistry(context);
    }

    @NotNull
    <T> ServiceRegistration<T> register(@NotNull ServiceKey<T> key, @NotNull T service);

    @NotNull
    <T> Optional<T> get(@NotNull ServiceKey<T> key);

    @NotNull
    <T> T require(@NotNull ServiceKey<T> key);

    boolean contains(@NotNull ServiceKey<?> key);

    @NotNull
    Set<ServiceKey<?>> keys();

    @NotNull
    List<ServiceDiagnostic> diagnostics();

    void loadAll();

    void enableAll();

    void disableAll();

    @Override
    default void close() {
        disableAll();
    }
}
