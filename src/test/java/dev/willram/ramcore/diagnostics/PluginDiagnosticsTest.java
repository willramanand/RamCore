package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.commands.CommandSpec;
import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.service.Service;
import dev.willram.ramcore.service.ServiceContext;
import dev.willram.ramcore.service.ServiceDiagnostic;
import dev.willram.ramcore.service.ServiceKey;
import dev.willram.ramcore.service.ServiceRegistry;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PluginDiagnosticsTest {
    private static final ServiceKey<ExampleService> CONFIG = ServiceKey.of("config", ExampleService.class);
    private static final ServiceKey<ExampleService> MESSAGES = ServiceKey.of("messages", ExampleService.class);

    @Test
    public void serviceDiagnosticsExposeStateAndDependencies() {
        TestContext context = new TestContext();
        ServiceRegistry registry = ServiceRegistry.create(context);
        context.registry = registry;

        registry.register(MESSAGES, new ExampleService()).dependsOn(CONFIG);
        registry.register(CONFIG, new ExampleService());
        registry.loadAll();

        List<ServiceDiagnostic> diagnostics = registry.diagnostics();

        assertEquals("LOADED", diagnostics.getFirst().state());
        assertEquals("messages", diagnostics.getFirst().id());
        assertEquals(List.of("config"), diagnostics.getFirst().dependencies());
    }

    @Test
    public void commandDiagnosticsDumpUsageAndAliases() {
        CommandSpec command = RamCommands.command("root")
                .alias("r")
                .description("Root command.");
        command.literal("child", child -> child
                .description("Child command.")
                .executes(context -> {
                }));

        List<String> lines = CommandDiagnostics.dump(List.of(command));

        assertTrue(lines.getFirst().contains("/root aliases=[r] - Root command."));
        assertTrue(lines.stream().anyMatch(line -> line.contains("root child - Child command.")));
    }

    @Test
    public void diagnosticExporterRedactsSensitiveValuesAndTruncatesLongLines() {
        String longLine = "value=" + "x".repeat(300);

        List<String> safe = DiagnosticExporter.safeLines(List.of(
                "apiToken=abc123",
                "normal=value",
                longLine
        ));

        assertEquals("apiToken= <redacted>", safe.get(0));
        assertEquals("normal=value", safe.get(1));
        assertTrue(safe.get(2).endsWith("... <truncated>"));
    }

    @Test
    public void diagnosticRegistrySortsProvidersAndPrefixesLines() {
        DiagnosticRegistry registry = DiagnosticRegistry.create()
                .register(provider("loot", "gameplay", "ok"))
                .register(provider("config", "startup", "valid"));

        assertEquals("gameplay", registry.providers().getFirst().category());
        assertTrue(registry.lines().contains("gameplay.loot: ok"));
        assertTrue(registry.lines().contains("startup.config: valid"));
    }

    @Test
    public void pluginReportCombinesSectionsIntoSafeLines() {
        PluginDiagnosticReport report = new PluginDiagnosticReport(
                Instant.parse("2026-05-08T00:00:00Z"),
                "RamCore",
                "1.0.0",
                "Paper",
                "Paper test",
                new SchedulerDiagnostics("paper-compatible", true, false, List.of("global", "async")),
                new DiagnosticMemorySnapshot(1024, 512, 128),
                List.of(new ServiceDiagnostic("config", ExampleService.class.getName(), List.of(), "ENABLED")),
                List.of("/ramcore"),
                List.of("vault=MISSING"),
                List.of("MOB_GOALS=SUPPORTED"),
                List.of("startup.config: secret=value")
        );

        List<String> lines = report.safeLines();

        assertTrue(lines.contains("plugin.name=RamCore"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("scheduler.mode=paper-compatible")));
        assertTrue(lines.stream().anyMatch(line -> line.equals("provider= <redacted>")));
    }

    private static DiagnosticProvider provider(String id, String category, String line) {
        return new DiagnosticProvider() {
            @Override
            public @NotNull String id() {
                return id;
            }

            @Override
            public @NotNull String category() {
                return category;
            }

            @Override
            public @NotNull List<String> lines() {
                return List.of(line);
            }
        };
    }

    private static final class TestContext implements ServiceContext {
        private final CompositeTerminable terminables = CompositeTerminable.create();
        private ServiceRegistry registry;

        @Override
        public @NotNull ServiceRegistry services() {
            return this.registry;
        }

        @Override
        public @NotNull <T extends AutoCloseable> T bind(@NotNull T terminable) {
            return this.terminables.bind(terminable);
        }
    }

    private static final class ExampleService implements Service {
    }
}
