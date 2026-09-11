package dev.willram.ramcore;

import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.config.BukkitConfig;
import dev.willram.ramcore.diagnostics.FoliaDiagnosticsCommandModule;
import dev.willram.ramcore.reload.ContentReloadService;
import dev.willram.ramcore.session.SessionRecorder;
import dev.willram.ramcore.update.UpdateChecker;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;

import java.time.Clock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RamCore extends RamPlugin {
    static final String DIAGNOSTICS_PROPERTY = "ramcore.diagnostics";

    private BukkitConfig config;
    private SessionRecorder sessionRecorder = SessionRecorder.NOOP;
    private final ContentReloadService reloadService = new ContentReloadService();

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
        if (this.config.get(RamCoreConfig.TIMELINE_ENABLED)) {
            this.sessionRecorder = SessionRecorder.ring(128, Clock.systemUTC(), Bukkit::getCurrentTick);
        }
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

    /**
     * The session recorder (NOOP unless diagnostics.timeline.enabled).
     *
     * @return the recorder
     */
    @NotNull
    public SessionRecorder sessionRecorder() {
        return this.sessionRecorder;
    }

    /**
     * The shared content reload service. Consumer plugins register their {@code ContentPack}s here so
     * {@code /ramcore diagnostics reload <pack>} can hot-reload them.
     *
     * @return the reload service
     */
    @NotNull
    public ContentReloadService reloadService() {
        return this.reloadService;
    }

    @Override
    public void registerCommands(@NotNull Commands commands) {
        if (!diagnosticsEnabled(System.getProperty(DIAGNOSTICS_PROPERTY))) {
            this.log("<yellow>RamCore diagnostics command is disabled by system property <white>" + DIAGNOSTICS_PROPERTY + "</white>.");
            return;
        }
        RamCommands.register(commands, new FoliaDiagnosticsCommandModule(this, this.sessionRecorder, this.reloadService));
    }

    static boolean diagnosticsEnabled(@Nullable String propertyValue) {
        return propertyValue == null || Boolean.parseBoolean(propertyValue);
    }
}
