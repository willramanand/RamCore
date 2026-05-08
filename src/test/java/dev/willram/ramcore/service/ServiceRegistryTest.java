package dev.willram.ramcore.service;

import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ServiceRegistryTest {

    private static final ServiceKey<RecordingService> CONFIG =
            ServiceKey.of("config", RecordingService.class);
    private static final ServiceKey<RecordingService> MESSAGES =
            ServiceKey.of("messages", RecordingService.class);
    private static final ServiceKey<RecordingService> COMMANDS =
            ServiceKey.of("commands", RecordingService.class);

    @Test
    public void lifecycleFollowsDependencyOrder() {
        TestContext context = new TestContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.registry = registry;

        registry.register(COMMANDS, new RecordingService("commands", context.events)).dependsOn(MESSAGES);
        registry.register(MESSAGES, new RecordingService("messages", context.events)).dependsOn(CONFIG);
        registry.register(CONFIG, new RecordingService("config", context.events));

        registry.loadAll();
        registry.enableAll();
        registry.disableAll();

        assertEquals(List.of(
                "config:load",
                "messages:load",
                "commands:load",
                "config:enable",
                "messages:enable",
                "commands:enable",
                "commands:disable",
                "messages:disable",
                "config:disable"
        ), context.events);
    }

    @Test
    public void lookupUsesTypedKey() {
        TestContext context = new TestContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.registry = registry;
        RecordingService config = new RecordingService("config", context.events);

        registry.register(CONFIG, config);

        assertTrue(registry.contains(CONFIG));
        assertSame(config, registry.require(CONFIG));
        assertSame(config, registry.get(CONFIG).orElseThrow());
    }

    @Test(expected = IllegalStateException.class)
    public void missingDependencyFailsFast() {
        TestContext context = new TestContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.registry = registry;

        registry.register(COMMANDS, new RecordingService("commands", context.events)).dependsOn(MESSAGES);

        registry.loadAll();
    }

    @Test(expected = IllegalStateException.class)
    public void cyclicDependencyFailsFast() {
        TestContext context = new TestContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.registry = registry;

        registry.register(CONFIG, new RecordingService("config", context.events)).dependsOn(COMMANDS);
        registry.register(COMMANDS, new RecordingService("commands", context.events)).dependsOn(CONFIG);

        registry.loadAll();
    }

    private static final class TestContext implements ServiceContext {
        private final List<String> events = new ArrayList<>();
        private final CompositeTerminable terminables = CompositeTerminable.create();
        private ServiceRegistry registry;

        @NotNull
        @Override
        public ServiceRegistry services() {
            return this.registry;
        }

        @NotNull
        @Override
        public <T extends AutoCloseable> T bind(@NotNull T terminable) {
            return this.terminables.bind(terminable);
        }
    }

    private record RecordingService(String name, List<String> events) implements Service {
        @Override
        public void load(@NotNull ServiceContext context) {
            this.events.add(this.name + ":load");
        }

        @Override
        public void enable(@NotNull ServiceContext context) {
            this.events.add(this.name + ":enable");
        }

        @Override
        public void disable(@NotNull ServiceContext context) {
            this.events.add(this.name + ":disable");
        }
    }
}
