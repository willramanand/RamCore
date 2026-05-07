package dev.willram.ramcore.commands;

import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public interface CommandModule {

    @NotNull
    Collection<CommandSpec> commands();

    @NotNull
    default Set<String> register(@NotNull Commands commands) {
        return RamCommands.register(commands, commands());
    }
}
