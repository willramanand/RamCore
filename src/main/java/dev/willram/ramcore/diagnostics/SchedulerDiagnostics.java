package dev.willram.ramcore.diagnostics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Scheduler mode and supported execution anchors.
 */
public record SchedulerDiagnostics(
        @NotNull String mode,
        boolean paperDetected,
        boolean foliaDetected,
        @NotNull List<String> supportedContexts
) {

    public SchedulerDiagnostics {
        supportedContexts = List.copyOf(supportedContexts);
    }

    @NotNull
    public static SchedulerDiagnostics capture() {
        boolean paper = hasClass("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
        boolean folia = hasClass("io.papermc.paper.threadedregions.RegionizedServer")
                || hasClass("io.papermc.paper.threadedregions.RegionizedWorldData");
        return new SchedulerDiagnostics(
                folia ? "folia-regionized" : "paper-compatible",
                paper,
                folia,
                List.of("global", "async", "entity", "player", "region", "block", "chunk")
        );
    }

    @NotNull
    public List<String> lines() {
        return List.of(
                "mode=" + this.mode,
                "paperDetected=" + this.paperDetected,
                "foliaDetected=" + this.foliaDetected,
                "contexts=" + this.supportedContexts
        );
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, SchedulerDiagnostics.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
