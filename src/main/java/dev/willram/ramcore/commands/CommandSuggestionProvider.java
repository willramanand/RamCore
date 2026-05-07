package dev.willram.ramcore.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@FunctionalInterface
public interface CommandSuggestionProvider {

    @NotNull
    Collection<CommandSuggestion> suggest(@NotNull CommandContext context, @NotNull String remaining) throws CommandInterruptException;
}
