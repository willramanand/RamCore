package dev.willram.ramcore.service;

import dev.willram.ramcore.testkit.TestServiceContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ServiceRegistryTest {

    private static final ServiceKey<RecordingService> CONFIG =
            ServiceKey.of("config", RecordingService.class);
    private static final ServiceKey<RecordingService> MESSAGES =
            ServiceKey.of("messages", RecordingService.class);
    private static final ServiceKey<RecordingService> COMMANDS =
            ServiceKey.of("commands", RecordingService.class);

    @Test
    public void lifecycleFollowsDependencyOrder() {
        TestServiceContext context = new TestServiceContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.attach(registry);

        registry.register(COMMANDS, new RecordingService("commands", context.events())).dependsOn(MESSAGES);
        registry.register(MESSAGES, new RecordingService("messages", context.events())).dependsOn(CONFIG);
        registry.register(CONFIG, new RecordingService("config", context.events()));

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
        ), context.events());
    }

    @Test
    public void lookupUsesTypedKey() {
        TestServiceContext context = new TestServiceContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.attach(registry);
        RecordingService config = new RecordingService("config", context.events());

        registry.register(CONFIG, config);

        assertTrue(registry.contains(CONFIG));
        assertSame(config, registry.require(CONFIG));
        assertSame(config, registry.get(CONFIG).orElseThrow());
    }

    @Test
    public void missingDependencyFailsFast() {
        TestServiceContext context = new TestServiceContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.attach(registry);

        registry.register(COMMANDS, new RecordingService("commands", context.events())).dependsOn(MESSAGES);

        assertThrows(IllegalStateException.class, registry::loadAll);
    }

    @Test
    public void cyclicDependencyFailsFast() {
        TestServiceContext context = new TestServiceContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.attach(registry);

        registry.register(CONFIG, new RecordingService("config", context.events())).dependsOn(COMMANDS);
        registry.register(COMMANDS, new RecordingService("commands", context.events())).dependsOn(CONFIG);

        assertThrows(IllegalStateException.class, registry::loadAll);
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
