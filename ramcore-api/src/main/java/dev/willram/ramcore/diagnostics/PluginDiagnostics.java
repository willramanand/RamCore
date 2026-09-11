package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.commands.CommandSpec;
import dev.willram.ramcore.integration.IntegrationRegistry;
import dev.willram.ramcore.integration.IntegrationSnapshot;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Factory methods for runtime plugin diagnostic reports.
 */
public final class PluginDiagnostics {

    @NotNull
    public static PluginDiagnosticReport capture(
            @NotNull RamPlugin plugin,
            @NotNull Collection<CommandSpec> commands,
            @NotNull IntegrationRegistry integrations,
            @NotNull NmsAccessRegistry nms,
            @NotNull DiagnosticRegistry providers
    ) {
        Objects.requireNonNull(plugin, "plugin");
        return new PluginDiagnosticReport(
                Instant.now(),
                plugin.getName(),
                plugin.getPluginMeta().getVersion(),
                Bukkit.getName(),
                Bukkit.getVersion(),
                SchedulerDiagnostics.capture(),
                DiagnosticMemorySnapshot.capture(),
                plugin.services().diagnostics(),
                CommandDiagnostics.dump(commands),
                integrationLines(integrations),
                nms.diagnostics().lines(),
                providers.lines()
        );
    }

    @NotNull
    public static List<String> integrationLines(@NotNull IntegrationRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return registry.snapshots().stream()
                .map(PluginDiagnostics::line)
                .toList();
    }

    private static String line(IntegrationSnapshot snapshot) {
        String version = snapshot.version() == null ? "unknown" : snapshot.version();
        return snapshot.descriptor().id() + "=" + snapshot.status()
                + " plugin=" + snapshot.descriptor().pluginName()
                + " version=" + version
                + " capabilities=" + snapshot.descriptor().capabilities()
                + " message=" + snapshot.message();
    }

    private PluginDiagnostics() {
    }
}
