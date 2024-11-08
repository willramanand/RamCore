package dev.willram.ramcore.commands;

import com.google.common.collect.ImmutableList;
import dev.willram.ramcore.commands.arguments.Argument;
import dev.willram.ramcore.commands.arguments.SimpleArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CommandContext {

    private final CommandSourceStack stack;
    private final CommandSender sender;
    private final Location location;
    private Player player;

    private final List<String> args;
    public String alias;

    public List<RamCommand> commandChain = new ArrayList<>();

    public CommandContext(@NotNull CommandSourceStack stack, List<String> args) {
        this.sender = stack.getSender();
        this.location = stack.getLocation();
        this.stack = stack;
        this.args = args;

        if (sender instanceof Player) {
            this.player = (Player) sender;
        }
    }

    @Nonnull
    public List<String> args() {
        return this.args;
    }

    @Nonnull
    public Argument arg(int index) {
        return new SimpleArgument(index, rawArg(index));
    }

    @Nullable
    public String rawArg(int index) {
        if (index < 0 || index >= this.args.size()) {
            return null;
        }
        return this.args.get(index);
    }

    @Nonnull
    public CommandSourceStack stack() {
        return stack;
    }

    @Nonnull
    public CommandSender sender() {
        return this.sender;
    }

    @Nullable
    public Player player() {
        return this.player;
    }

    @Nullable
    public Location location() {
        return this.location;
    }

    public void msg(String message) {
        sender.sendRichMessage(message);
    }

    public void msg(String... messages) {
        for (String message : messages) {
            msg(message);
        }
    }

    public void msg(Component component) {
        sender.sendMessage(component);
    }

    public void msgOther(Player player, String message) {
        player.sendRichMessage(message);
    }

    public void msgOther(Player player, String... messages) {
        for (String message : messages) {
            msgOther(player, message);
        }
    }

    public void msgOther(Player player, Component component) {
        player.sendMessage(component);
    }

    // Check is set
    public boolean argIsSet(int index) {
        return args.size() >= index + 1;
    }


}