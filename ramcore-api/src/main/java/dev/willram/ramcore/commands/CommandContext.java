package dev.willram.ramcore.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.willram.ramcore.permission.PermissionNode;
import dev.willram.ramcore.permission.PermissionRequirement;
import dev.willram.ramcore.permission.Permissions;
import dev.willram.ramcore.text.TextContext;
import dev.willram.ramcore.text.Texts;
import io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class CommandContext {

    private final CommandSourceStack stack;
    private final com.mojang.brigadier.context.CommandContext<CommandSourceStack> brigadier;
    private final CommandSender sender;
    private final Location location;
    private final Player player;

    public CommandContext(@NotNull CommandSourceStack stack) {
        this.sender = stack.getSender();
        this.location = stack.getLocation();
        this.stack = stack;
        this.brigadier = null;

        if (sender instanceof Player) {
            this.player = (Player) sender;
        } else {
            this.player = null;
        }
    }

    public CommandContext(@NotNull com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        this.brigadier = context;
        this.stack = context.getSource();
        this.sender = this.stack.getSender();
        this.location = this.stack.getLocation();

        if (this.sender instanceof Player) {
            this.player = (Player) this.sender;
        } else {
            this.player = null;
        }
    }

    @NotNull
    public CommandSourceStack stack() {
        return stack;
    }

    @NotNull
    public CommandSender sender() {
        return this.sender;
    }

    @Nullable
    public Player player() {
        return this.player;
    }

    @NotNull
    public Player playerOrFail() throws CommandInterruptException {
        if (this.player == null) {
            throw playerOnly();
        }
        return this.player;
    }

    @NotNull
    public Player requirePlayer() throws CommandInterruptException {
        return playerOrFail();
    }

    @Nullable
    public Location location() {
        return this.location;
    }

    @Nullable
    public Entity executor() {
        return this.stack.getExecutor();
    }

    @Nullable
    public com.mojang.brigadier.context.CommandContext<CommandSourceStack> brigadier() {
        return this.brigadier;
    }

    @NotNull
    public String input() {
        return this.brigadier == null ? "" : this.brigadier.getInput();
    }

    public boolean has(@NotNull String name) {
        if (this.brigadier == null) {
            return false;
        }

        try {
            this.brigadier.getArgument(name, Object.class);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean hasPermission(@NotNull String permission) {
        return this.sender.hasPermission(Objects.requireNonNull(permission, "permission"));
    }

    public boolean hasPermission(@NotNull PermissionNode permission) {
        return Permissions.has(this.sender, permission);
    }

    public boolean hasPermissions(@NotNull PermissionRequirement requirement) {
        return Permissions.has(this.sender, requirement);
    }

    public void requirePermission(@NotNull String permission) throws CommandInterruptException {
        if (!hasPermission(permission)) {
            throw noPermission();
        }
    }

    public void requirePermission(@NotNull PermissionNode permission) throws CommandInterruptException {
        if (!hasPermission(permission)) {
            throw fail(permission.denialMessage());
        }
    }

    public void requirePermissions(@NotNull PermissionRequirement requirement) throws CommandInterruptException {
        if (!hasPermissions(requirement)) {
            throw fail(requirement.denialMessage());
        }
    }

    public <T> boolean has(@NotNull CommandArgument<T> argument) {
        return has(argument.name());
    }

    public <T, R extends ArgumentResolver<T>> boolean has(@NotNull ResolvedCommandArgument<T, R> argument) {
        return has(argument.name());
    }

    @NotNull
    public <T> T get(@NotNull String name, @NotNull Class<T> type) throws CommandInterruptException {
        if (this.brigadier == null) {
            throw new CommandInterruptException("<red>This command did not parse a Brigadier argument named <white>" + name + "</white>.");
        }

        try {
            return this.brigadier.getArgument(name, type);
        } catch (IllegalArgumentException e) {
            throw new CommandInterruptException("<red>Missing or invalid command argument: <white>" + name + "</white>");
        }
    }

    @NotNull
    public <T> T get(@NotNull CommandArgument<T> argument) throws CommandInterruptException {
        Objects.requireNonNull(argument, "argument");
        return get(argument.name(), argument.valueType());
    }

    @NotNull
    public <T> Optional<T> optional(@NotNull CommandArgument<T> argument) throws CommandInterruptException {
        Objects.requireNonNull(argument, "argument");
        return has(argument) ? Optional.of(get(argument)) : Optional.empty();
    }

    @NotNull
    public <T, R extends ArgumentResolver<T>> T resolve(@NotNull String name, @NotNull Class<R> resolverType) throws CommandInterruptException {
        R resolver = get(name, resolverType);
        try {
            return resolver.resolve(this.stack);
        } catch (CommandSyntaxException e) {
            throw new CommandInterruptException("<red>" + e.getMessage());
        }
    }

    @NotNull
    public <T, R extends ArgumentResolver<T>> T resolve(@NotNull ResolvedCommandArgument<T, R> argument) throws CommandInterruptException {
        Objects.requireNonNull(argument, "argument");
        return resolve(argument.name(), argument.resolverType());
    }

    @NotNull
    public <T, R extends ArgumentResolver<T>> Optional<T> optional(@NotNull ResolvedCommandArgument<T, R> argument) throws CommandInterruptException {
        Objects.requireNonNull(argument, "argument");
        return has(argument) ? Optional.of(resolve(argument)) : Optional.empty();
    }

    @NotNull
    public List<Player> players(@NotNull String name) throws CommandInterruptException {
        return resolve(name, PlayerSelectorArgumentResolver.class);
    }

    @NotNull
    public Player player(@NotNull String name) throws CommandInterruptException {
        List<Player> players = players(name);
        if (players.size() != 1) {
            throw new CommandInterruptException("<red>Expected exactly one player.");
        }
        return players.get(0);
    }

    @NotNull
    public Player player(@NotNull ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> argument) throws CommandInterruptException {
        Objects.requireNonNull(argument, "argument");
        return player(argument.name());
    }

    @NotNull
    public List<Player> players(@NotNull ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> argument) throws CommandInterruptException {
        return resolve(argument);
    }

    @NotNull
    public List<Entity> entities(@NotNull String name) throws CommandInterruptException {
        return resolve(name, EntitySelectorArgumentResolver.class);
    }

    @NotNull
    public Entity entity(@NotNull String name) throws CommandInterruptException {
        List<Entity> entities = entities(name);
        if (entities.size() != 1) {
            throw new CommandInterruptException("<red>Expected exactly one entity.");
        }
        return entities.get(0);
    }

    @NotNull
    public Entity entity(@NotNull ResolvedCommandArgument<List<Entity>, EntitySelectorArgumentResolver> argument) throws CommandInterruptException {
        Objects.requireNonNull(argument, "argument");
        return entity(argument.name());
    }

    @NotNull
    public List<Entity> entities(@NotNull ResolvedCommandArgument<List<Entity>, EntitySelectorArgumentResolver> argument) throws CommandInterruptException {
        return resolve(argument);
    }

    @NotNull
    public CommandInterruptException fail(@NotNull String message) {
        return new CommandInterruptException(message);
    }

    @NotNull
    public CommandInterruptException noPermission() {
        return fail("<red>You do not have permission to use this command.");
    }

    @NotNull
    public CommandInterruptException playerOnly() {
        return fail("<red>Only a player can execute this command.");
    }

    public void assertTrue(boolean condition, @NotNull String message) throws CommandInterruptException {
        if (!condition) {
            throw fail(message);
        }
    }

    public void usage(@NotNull CommandSpec spec) {
        Objects.requireNonNull(spec, "spec").sendUsage(this);
    }

    public void msg(String message) {
        sender.sendRichMessage(message);
    }

    public void msg(String message, TextContext context) {
        sender.sendMessage(Texts.render(message, context));
    }

    public void reply(String message) {
        msg(message);
    }

    public void reply(String message, TextContext context) {
        msg(message, context);
    }

    public void msg(String... messages) {
        for (String message : messages) {
            msg(message);
        }
    }

    public void msg(Component component) {
        sender.sendMessage(component);
    }

    public void reply(Component component) {
        msg(component);
    }

    public void msgOther(Player player, String message) {
        player.sendRichMessage(message);
    }

    public void msgOther(Player player, String message, TextContext context) {
        player.sendMessage(Texts.render(message, context));
    }

    public void msgOther(Player player, String... messages) {
        for (String message : messages) {
            msgOther(player, message);
        }
    }

    public void msgOther(Player player, Component component) {
        player.sendMessage(component);
    }
}
