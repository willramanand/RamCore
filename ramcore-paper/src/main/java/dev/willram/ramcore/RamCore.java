package dev.willram.ramcore;

import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.config.BukkitConfig;
import dev.willram.ramcore.diagnostics.FoliaDiagnosticsCommandModule;
import dev.willram.ramcore.update.UpdateChecker;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RamCore extends RamPlugin {
    static final String DIAGNOSTICS_PROPERTY = "ramcore.diagnostics";

    private BukkitConfig config;

    @Override
    public void enable() {
        if (this.config.get(RamCoreConfig.METRICS_ENABLED)) {
            try {
                RamCoreMetrics.start(this);
            } catch (RuntimeException e) {
                this.log("<yellow>bStats metrics failed to start: <white>" + e.getMessage() + "</white>.");
            }
        }
        if (this.config.get(RamCoreConfig.UPDATE_ENABLED)) {
            UpdateChecker.check(getPluginMeta().getVersion(), this.config.get(RamCoreConfig.UPDATE_REPO));
        }
    }

    @Override
    public void disable() {

    }

    @Override
    public void load() {
        this.config = RamCoreConfig.load(getDataFolder().toPath());
    }

    /**
     * The loaded RamCore config.
     *
     * @return the config
     */
    @NotNull
    public BukkitConfig config() {
        return this.config;
    }

    @Override
    public void registerCommands(@NotNull Commands commands) {
        if (!diagnosticsEnabled(System.getProperty(DIAGNOSTICS_PROPERTY))) {
            this.log("<yellow>RamCore diagnostics command is disabled by system property <white>" + DIAGNOSTICS_PROPERTY + "</white>.");
            return;
        }
        RamCommands.register(commands, new FoliaDiagnosticsCommandModule(this));
    }

    static boolean diagnosticsEnabled(@Nullable String propertyValue) {
        return propertyValue == null || Boolean.parseBoolean(propertyValue);
    }
}
