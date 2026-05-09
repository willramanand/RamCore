package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.service.ServiceDiagnostic;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pasteable plugin diagnostics assembled from RamCore subsystems.
 */
public record PluginDiagnosticReport(
        @NotNull Instant generatedAt,
        @NotNull String pluginName,
        @NotNull String pluginVersion,
        @NotNull String serverName,
        @NotNull String serverVersion,
        @NotNull SchedulerDiagnostics scheduler,
        @NotNull DiagnosticMemorySnapshot memory,
        @NotNull List<ServiceDiagnostic> services,
        @NotNull List<String> commandTree,
        @NotNull List<String> integrations,
        @NotNull List<String> nms,
        @NotNull List<String> providers
) {

    public PluginDiagnosticReport {
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(pluginVersion, "pluginVersion");
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(serverVersion, "serverVersion");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(memory, "memory");
        services = List.copyOf(Objects.requireNonNull(services, "services"));
        commandTree = List.copyOf(Objects.requireNonNull(commandTree, "commandTree"));
        integrations = List.copyOf(Objects.requireNonNull(integrations, "integrations"));
        nms = List.copyOf(Objects.requireNonNull(nms, "nms"));
        providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    @NotNull
    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("generatedAt=" + this.generatedAt);
        lines.add("plugin.name=" + this.pluginName);
        lines.add("plugin.version=" + this.pluginVersion);
        lines.add("server.name=" + this.serverName);
        lines.add("server.version=" + this.serverVersion);
        this.scheduler.lines().forEach(line -> lines.add("scheduler." + line));
        this.memory.lines().forEach(lines::add);
        this.services.forEach(service -> lines.add("service." + service.id() + "=" + service.state()
                + " type=" + service.type()
                + " dependsOn=" + service.dependencies()));
        this.commandTree.forEach(line -> lines.add("command=" + line));
        this.integrations.forEach(line -> lines.add("integration=" + line));
        this.nms.forEach(line -> lines.add("nms=" + line));
        this.providers.forEach(line -> lines.add("provider=" + line));
        return List.copyOf(lines);
    }

    @NotNull
    public List<String> safeLines() {
        return DiagnosticExporter.safeLines(lines());
    }
}
