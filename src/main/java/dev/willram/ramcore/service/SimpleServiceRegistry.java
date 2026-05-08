package dev.willram.ramcore.service;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class SimpleServiceRegistry implements ServiceRegistry {
    private enum State {
        NEW,
        LOADED,
        ENABLED,
        DISABLED
    }

    private final ServiceContext context;
    private final Map<ServiceKey<?>, Registration<?>> registrations = new LinkedHashMap<>();
    private final List<Registration<?>> lifecycleOrder = new ArrayList<>();
    private State state = State.NEW;

    SimpleServiceRegistry(@NotNull ServiceContext context) {
        this.context = requireNonNull(context, "context");
    }

    @NotNull
    @Override
    public <T> ServiceRegistration<T> register(@NotNull ServiceKey<T> key, @NotNull T service) {
        requireNonNull(key, "key");
        requireNonNull(service, "service");

        if (this.state != State.NEW) {
            throw new IllegalStateException("services cannot be registered after load");
        }

        if (this.registrations.containsKey(key)) {
            throw new IllegalArgumentException("service already registered: " + key);
        }

        if (!key.type().isInstance(service)) {
            throw new IllegalArgumentException("service " + key + " is not instance of " + key.type().getName());
        }

        Registration<T> registration = new Registration<>(key, service);
        this.registrations.put(key, registration);
        return registration;
    }

    @NotNull
    @Override
    public <T> Optional<T> get(@NotNull ServiceKey<T> key) {
        Registration<?> registration = this.registrations.get(requireNonNull(key, "key"));
        if (registration == null) {
            return Optional.empty();
        }

        return Optional.of(key.type().cast(registration.service()));
    }

    @NotNull
    @Override
    public <T> T require(@NotNull ServiceKey<T> key) {
        return get(key).orElseThrow(() -> new IllegalStateException("service not registered: " + key));
    }

    @Override
    public boolean contains(@NotNull ServiceKey<?> key) {
        return this.registrations.containsKey(requireNonNull(key, "key"));
    }

    @NotNull
    @Override
    public Set<ServiceKey<?>> keys() {
        return Collections.unmodifiableSet(this.registrations.keySet());
    }

    @Override
    public void loadAll() {
        if (this.state != State.NEW) {
            throw new IllegalStateException("services already loaded");
        }

        this.lifecycleOrder.clear();
        this.lifecycleOrder.addAll(resolveLifecycleOrder());
        for (Registration<?> registration : this.lifecycleOrder) {
            if (registration.service() instanceof Service service) {
                service.load(this.context);
            }
        }

        this.state = State.LOADED;
    }

    @Override
    public void enableAll() {
        if (this.state == State.NEW) {
            loadAll();
        }

        if (this.state != State.LOADED) {
            throw new IllegalStateException("services cannot be enabled from state " + this.state);
        }

        int enabled = 0;
        try {
            for (Registration<?> registration : this.lifecycleOrder) {
                if (registration.service() instanceof Service service) {
                    service.enable(this.context);
                }
                enabled++;
            }
        } catch (RuntimeException e) {
            disableEnabled(enabled);
            this.state = State.DISABLED;
            throw e;
        }

        this.state = State.ENABLED;
    }

    @Override
    public void disableAll() {
        if (this.state == State.DISABLED || this.state == State.NEW) {
            this.state = State.DISABLED;
            return;
        }

        disableEnabled(this.lifecycleOrder.size());
        this.state = State.DISABLED;
    }

    private void disableEnabled(int enabled) {
        RuntimeException first = null;
        for (int i = enabled - 1; i >= 0; i--) {
            Object service = this.lifecycleOrder.get(i).service();

            try {
                if (service instanceof Service lifecycleService) {
                    lifecycleService.disable(this.context);
                }

                if (service instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            } catch (Exception e) {
                if (first == null) {
                    first = e instanceof RuntimeException runtimeException
                            ? runtimeException
                            : new IllegalStateException("service shutdown failed", e);
                } else {
                    first.addSuppressed(e);
                }
            }
        }

        if (first != null) {
            throw first;
        }
    }

    private List<Registration<?>> resolveLifecycleOrder() {
        List<Registration<?>> ordered = new ArrayList<>();
        Set<ServiceKey<?>> visiting = new LinkedHashSet<>();
        Set<ServiceKey<?>> visited = new LinkedHashSet<>();

        for (ServiceKey<?> key : this.registrations.keySet()) {
            visit(key, visiting, visited, ordered, new ArrayDeque<>());
        }

        return ordered;
    }

    private void visit(
            ServiceKey<?> key,
            Set<ServiceKey<?>> visiting,
            Set<ServiceKey<?>> visited,
            List<Registration<?>> ordered,
            Deque<ServiceKey<?>> path
    ) {
        if (visited.contains(key)) {
            return;
        }

        Registration<?> registration = this.registrations.get(key);
        if (registration == null) {
            throw new IllegalStateException("missing service dependency: " + key);
        }

        if (!visiting.add(key)) {
            path.addLast(key);
            throw new IllegalStateException("cyclic service dependency: " + path);
        }

        path.addLast(key);
        for (ServiceKey<?> dependency : registration.dependencies()) {
            if (!this.registrations.containsKey(dependency)) {
                throw new IllegalStateException("service " + key + " depends on missing service " + dependency);
            }
            visit(dependency, visiting, visited, ordered, path);
        }
        path.removeLast();

        visiting.remove(key);
        visited.add(key);
        ordered.add(registration);
    }

    private static final class Registration<T> implements ServiceRegistration<T> {
        private final ServiceKey<T> key;
        private final T service;
        private final Set<ServiceKey<?>> dependencies = new LinkedHashSet<>();

        private Registration(ServiceKey<T> key, T service) {
            this.key = key;
            this.service = service;
        }

        @NotNull
        @Override
        public ServiceRegistration<T> dependsOn(@NotNull ServiceKey<?> dependency) {
            this.dependencies.add(requireNonNull(dependency, "dependency"));
            return this;
        }

        @NotNull
        @Override
        public ServiceKey<T> key() {
            return this.key;
        }

        @NotNull
        @Override
        public T service() {
            return this.service;
        }

        private Set<ServiceKey<?>> dependencies() {
            return this.dependencies;
        }
    }
}
