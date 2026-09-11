package dev.willram.ramcore.commands;

@FunctionalInterface
public interface CommandExecutor {

    void execute(CommandContext context) throws CommandInterruptException;
}
