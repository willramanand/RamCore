package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.commands.CommandSpec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Renders RamCore command specs into stable diagnostic lines.
 */
public final class CommandDiagnostics {

    @NotNull
    public static List<String> dump(@NotNull Collection<CommandSpec> commands) {
        Objects.requireNonNull(commands, "commands");
        List<String> lines = new ArrayList<>();
        for (CommandSpec command : commands) {
            Objects.requireNonNull(command, "command");
            lines.add("/" + command.label() + aliases(command) + description(command));
            for (String usage : command.usage()) {
                lines.add("  " + usage);
            }
        }
        return List.copyOf(lines);
    }

    private static String aliases(CommandSpec command) {
        return command.aliases().isEmpty() ? "" : " aliases=" + command.aliases();
    }

    private static String description(CommandSpec command) {
        return command.description().isBlank() ? "" : " - " + command.description();
    }

    private CommandDiagnostics() {
    }
}
