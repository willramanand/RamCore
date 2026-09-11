package dev.willram.ramcore.testkit;

import dev.willram.ramcore.service.ServiceContext;
import dev.willram.ramcore.service.ServiceRegistry;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link ServiceContext} for tests: owns a {@link CompositeTerminable}, exposes the registry it was
 * attached to, and carries an event log that recording services can append to.
 */
public final class TestServiceContext implements ServiceContext {
    private final List<String> events = new ArrayList<>();
    private final CompositeTerminable terminables = CompositeTerminable.create();
    private ServiceRegistry registry;

    /** Creates a context and a registry bound to it. */
    @NotNull
    public static TestServiceContext withRegistry() {
        TestServiceContext context = new TestServiceContext();
        context.attach(ServiceRegistry.create(context));
        return context;
    }

    public void attach(@NotNull ServiceRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @NotNull
    public List<String> events() {
        return this.events;
    }

    @NotNull
    public CompositeTerminable terminables() {
        return this.terminables;
    }

    @NotNull
    @Override
    public ServiceRegistry services() {
        if (this.registry == null) {
            throw new IllegalStateException("no registry attached; call attach(registry) or use withRegistry()");
        }
        return this.registry;
    }

    @NotNull
    @Override
    public <T extends AutoCloseable> T bind(@NotNull T terminable) {
        return this.terminables.bind(terminable);
    }
}
