package dev.willram.ramcore.commands;

import com.google.common.base.Splitter;
import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.utils.LoaderUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public abstract class RamCommand implements BasicCommand {


    public List<String> aliases;
    private final String permission;
    private final String usage;
    private final boolean enabled;
    private final boolean playerOnly;
    public List<RamCommand> subCommands;

    public RamCommand(boolean enabled, boolean playerOnly, String permission, String usage) {
        this.enabled = enabled;
        this.usage = usage;
        this.playerOnly = playerOnly;
        this.permission = permission;
        this.aliases = new ArrayList<>();
        this.subCommands = new ArrayList<>();
    }

    public abstract void perform(CommandContext context);

    public abstract List<String> tabCompletes(CommandContext context);

    public boolean validCall(CommandContext context) {
        if (!(context.sender() instanceof Player) && playerOnly) {
            context.msg("<red>Only a player can execute this command!");
            return false;
        }

        // Check our perms
        if (permission != null && !(context.sender().hasPermission(permission))) {
            context.msg("<red>You do not have permission for this command!");
            return false;
        }

        // Check spigot perms
        if (this.permission() != null && !(context.sender().hasPermission(this.permission()))) {
            context.msg("<red>You do not have permission for this command!");
            return false;
        }

        // If there are no args there is no point going on
        if (usage.isEmpty()) return true;

        List<String> usageParts = Splitter.on(" ").splitToList(usage);
        int totalArgs = usageParts.size();
        int requiredArgs = 0;
        for (String usagePart : usageParts) {
            if (!usagePart.startsWith("[") && !usagePart.endsWith("]")) {
                // assume it's a required argument
                requiredArgs++;
            }
        }

        if (requiredArgs != 0 && requiredArgs > context.args().size()) {
            context.msg("<red>Usage: " + this.usage);
            return false;
        }

        return true;
    }

    public void executeExt(CommandContext context) {
        // Is there a matching sub command?
        if (!context.args().isEmpty()) {
            for (RamCommand subCommand : this.subCommands) {
                if (subCommand.aliases.contains(context.args().getFirst().toLowerCase())) {
                    context.args().removeFirst();
                    context.commandChain.add(this);
                    subCommand.executeExt(context);
                    return;
                }
            }
        }

        if (!validCall(context)) {
            return;
        }

        if (!this.enabled) {
            context.msg("<red>This command is not enabled!");
            return;
        }

        try {
            perform(context);
        } catch (Exception e) {
            context.msg("<red>An error occurred while processing this command!");
            LoaderUtils.getPlugin().log("<red>An unhandled exception occurred while processing command!");
        }
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        this.executeExt(new CommandContext(stack, new ArrayList<>(Arrays.asList(args))));
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!subCommands.isEmpty()) {
            if (args.length <= 1) {
                List<String> allowAliases = new ArrayList<>();
                for (RamCommand subCommand : subCommands) {
                    if (subCommand.permission() == null) {
                        allowAliases.addAll(subCommand.aliases);
                    } else if (stack.getSender().hasPermission(subCommand.permission())) {
                        allowAliases.addAll(subCommand.aliases);
                    }
                }
                return allowAliases;
            }

            if (args.length >= 2) {
                for (RamCommand subCommand : subCommands) {
                    if (subCommand.aliases.contains(args[1].toLowerCase())) {
                        List<String> modifiedArgs = Arrays.stream(Arrays.copyOfRange(args, 1, args.length)).toList();
                        return subCommand.tabCompletes(new CommandContext(stack, modifiedArgs));
                    }
                }
            }
        }
        return this.tabCompletes(new CommandContext(stack, new ArrayList<>(Arrays.asList(args))));
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {

        if (!(sender instanceof Player) && playerOnly) {
            return false;
        }

        // Check perms
        return permission == null || sender.hasPermission(permission);
    }

    @Override
    public @Nullable String permission() {
        return this.permission;
    }
}
