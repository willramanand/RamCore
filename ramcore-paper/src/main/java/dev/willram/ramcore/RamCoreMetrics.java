package dev.willram.ramcore;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

/**
 * bStats wiring. The plugin id {@code 33973} is compiled in; {@code metrics.enabled} is the opt-out.
 * bStats is shaded and relocated (it refuses to run otherwise).
 */
final class RamCoreMetrics {
    private static final int PLUGIN_ID = 33973;

    private RamCoreMetrics() {
    }

    static void start(@NotNull RamCore plugin) {
        Metrics metrics = new Metrics(plugin, PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("server_type", () -> foliaPresent() ? "Folia" : "Paper"));
        metrics.addCustomChart(new SimplePie("redis_messaging", () -> plugin.config().get(RamCoreConfig.REDIS_ENABLED) ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("sql_storage", () -> plugin.config().get(RamCoreConfig.SQL_ENABLED) ? "enabled" : "disabled"));
    }

    private static boolean foliaPresent() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException notFolia) {
            return false;
        }
    }
}
