package dev.willram.ramcore;

import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.diagnostics.FoliaDiagnosticsCommandModule;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RamCore extends RamPlugin {
    static final String DIAGNOSTICS_PROPERTY = "ramcore.diagnostics";

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void load() {

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
