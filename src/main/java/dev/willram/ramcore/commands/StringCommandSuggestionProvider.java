package dev.willram.ramcore.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@FunctionalInterface
public interface StringCommandSuggestionProvider {

    @NotNull
    Collection<String> suggest(@NotNull CommandContext context, @NotNull String remaining) throws CommandInterruptException;
}
