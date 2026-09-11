package dev.willram.ramcore.commands;

import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class RamCommands {

    @NotNull
    public static CommandSpec command(@NotNull String label) {
        return CommandSpec.command(label);
    }

    @NotNull
    public static CommandModule module(@NotNull CommandSpec... specs) {
        List<CommandSpec> commands = Arrays.stream(specs)
                .map(spec -> Objects.requireNonNull(spec, "spec"))
                .toList();
        return () -> commands;
    }

    @NotNull
    public static Set<String> register(@NotNull Commands commands, @NotNull CommandSpec... specs) {
        Objects.requireNonNull(commands, "commands");
        LinkedHashSet<String> registered = new LinkedHashSet<>();
        for (CommandSpec spec : specs) {
            registered.addAll(Objects.requireNonNull(spec, "spec").register(commands));
        }
        return Set.copyOf(registered);
    }

    @NotNull
    public static Set<String> register(@NotNull Commands commands, @NotNull Collection<CommandSpec> specs) {
        Objects.requireNonNull(commands, "commands");
        Objects.requireNonNull(specs, "specs");
        LinkedHashSet<String> registered = new LinkedHashSet<>();
        for (CommandSpec spec : specs) {
            registered.addAll(Objects.requireNonNull(spec, "spec").register(commands));
        }
        return Set.copyOf(registered);
    }

    @NotNull
    public static Set<String> register(@NotNull Commands commands, @NotNull CommandModule... modules) {
        Objects.requireNonNull(commands, "commands");
        LinkedHashSet<String> registered = new LinkedHashSet<>();
        for (CommandModule module : modules) {
            registered.addAll(Objects.requireNonNull(module, "module").register(commands));
        }
        return Set.copyOf(registered);
    }

    @NotNull
    public static Set<String> registerModules(@NotNull Commands commands, @NotNull Collection<? extends CommandModule> modules) {
        Objects.requireNonNull(commands, "commands");
        Objects.requireNonNull(modules, "modules");
        LinkedHashSet<String> registered = new LinkedHashSet<>();
        for (CommandModule module : modules) {
            registered.addAll(Objects.requireNonNull(module, "module").register(commands));
        }
        return Set.copyOf(registered);
    }

    private RamCommands() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
