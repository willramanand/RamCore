package dev.willram.ramcore;

import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.diagnostics.FoliaDiagnosticsCommandModule;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

public final class RamCore extends RamPlugin {

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
        RamCommands.register(commands, new FoliaDiagnosticsCommandModule(this));
    }
}
