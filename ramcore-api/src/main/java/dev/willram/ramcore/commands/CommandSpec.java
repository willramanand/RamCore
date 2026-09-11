package dev.willram.ramcore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.willram.ramcore.cooldown.Cooldown;
import dev.willram.ramcore.permission.PermissionNode;
import dev.willram.ramcore.permission.PermissionRequirement;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.utils.LoaderUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public final class CommandSpec {
    private final String label;
    private final List<String> aliases = new ArrayList<>();
    private final Node root;
    private String description = "";

    private CommandSpec(@NotNull String label) {
        this.label = validateName(label, "label");
        this.root = new Node(Commands.literal(this.label), this.label);
    }

    @NotNull
    public static CommandSpec command(@NotNull String label) {
        return new CommandSpec(label);
    }

    @NotNull
    public CommandSpec description(@NotNull String description) {
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }

    @NotNull
    public CommandSpec alias(@NotNull String alias) {
        this.aliases.add(validateName(alias, "alias"));
        return this;
    }

    @NotNull
    public CommandSpec aliases(@NotNull String... aliases) {
        Arrays.stream(aliases).forEach(this::alias);
        return this;
    }

    @NotNull
    public CommandSpec aliases(@NotNull Collection<String> aliases) {
        Objects.requireNonNull(aliases, "aliases").forEach(this::alias);
        return this;
    }

    @NotNull
    public CommandSpec permission(@NotNull String permission) {
        this.root.permission(permission);
        return this;
    }

    @NotNull
    public CommandSpec permission(@NotNull PermissionNode permission) {
        this.root.permission(permission);
        return this;
    }

    @NotNull
    public CommandSpec permissions(@NotNull PermissionRequirement requirement) {
        this.root.permissions(requirement);
        return this;
    }

    @NotNull
    public CommandSpec playerOnly() {
        this.root.playerOnly();
        return this;
    }

    @NotNull
    public CommandSpec requires(@NotNull Predicate<CommandSourceStack> requirement) {
        this.root.requires(requirement);
        return this;
    }

    @NotNull
    public CommandSpec executes(@NotNull CommandExecutor executor) {
        this.root.executes(executor);
        return this;
    }

    @NotNull
    public CommandSpec executesAsync(@NotNull CommandExecutor executor) {
        this.root.executesAsync(executor);
        return this;
    }

    @NotNull
    public Node root() {
        return this.root;
    }

    @NotNull
    public CommandSpec withHelp() {
        return help("help");
    }

    @NotNull
    public CommandSpec help(@NotNull String literal) {
        this.root.literal(literal, help -> help
                .description("Show command usage.")
                .executes(this::sendUsage));
        return this;
    }

    @NotNull
    public CommandSpec literal(@NotNull String name, @NotNull Consumer<Node> configure) {
        this.root.literal(name, configure);
        return this;
    }

    @NotNull
    public Node literal(@NotNull String name) {
        return this.root.literal(name);
    }

    @NotNull
    public <T> CommandSpec argument(@NotNull String name, @NotNull ArgumentType<T> type, @NotNull Consumer<Node> configure) {
        this.root.argument(name, type, configure);
        return this;
    }

    @NotNull
    public <T> Node argument(@NotNull String name, @NotNull ArgumentType<T> type) {
        return this.root.argument(name, type);
    }

    @NotNull
    public <T> CommandSpec argument(@NotNull CommandArgument<T> argument, @NotNull Consumer<Node> configure) {
        this.root.argument(argument, configure);
        return this;
    }

    @NotNull
    public <T> Node argument(@NotNull CommandArgument<T> argument) {
        return this.root.argument(argument);
    }

    @NotNull
    public <T, R extends io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver<T>> CommandSpec argument(
            @NotNull ResolvedCommandArgument<T, R> argument,
            @NotNull Consumer<Node> configure
    ) {
        this.root.argument(argument, configure);
        return this;
    }

    @NotNull
    public <T, R extends io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver<T>> Node argument(
            @NotNull ResolvedCommandArgument<T, R> argument
    ) {
        return this.root.argument(argument);
    }

    @NotNull
    public LiteralCommandNode<CommandSourceStack> build() {
        return (LiteralCommandNode<CommandSourceStack>) this.root.buildNode();
    }

    @NotNull
    public Set<String> register(@NotNull Commands commands) {
        Objects.requireNonNull(commands, "commands");
        return commands.register(build(), this.description, this.aliases);
    }

    @NotNull
    public String label() {
        return this.label;
    }

    @NotNull
    public List<String> aliases() {
        return List.copyOf(this.aliases);
    }

    @NotNull
    public String description() {
        return this.description;
    }

    @NotNull
    public List<String> usage() {
        List<String> lines = new ArrayList<>();
        this.root.collectUsageStrings("", lines);
        return List.copyOf(lines);
    }

    public void sendUsage(@NotNull CommandContext context) {
        Objects.requireNonNull(context, "context");
        context.msg(Component.text("Command Help", NamedTextColor.GOLD)
                .append(Component.text(" /" + this.label, NamedTextColor.YELLOW)));

        if (!this.description.isBlank()) {
            context.msg(Component.text(this.description, NamedTextColor.GRAY));
        }

        if (!this.aliases.isEmpty()) {
            context.msg(Component.text("Aliases: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(String.join(", ", this.aliases.stream()
                            .map(alias -> "/" + alias)
                            .toList()), NamedTextColor.GRAY)));
        }

        List<UsageLine> lines = usageLines(context.stack());
        if (lines.isEmpty()) {
            context.msg(Component.text("No visible usage is registered for /" + this.label + ".", NamedTextColor.RED));
            return;
        }

        context.msg(Component.text("Usage:", NamedTextColor.YELLOW));
        for (UsageLine line : lines) {
            Component message = Component.text("  /", NamedTextColor.DARK_GRAY)
                    .append(Component.text(line.syntax(), NamedTextColor.AQUA));
            if (!line.description().isBlank()) {
                message = message
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(line.description(), NamedTextColor.GRAY));
            }
            context.msg(message);
            for (String example : line.examples()) {
                context.msg(Component.text("      e.g. ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(example, NamedTextColor.GRAY)));
            }
        }
    }

    @NotNull
    private List<UsageLine> usageLines(@NotNull CommandSourceStack source) {
        List<UsageLine> lines = new ArrayList<>();
        this.root.collectVisibleUsageLines("", source, lines);
        return List.copyOf(lines);
    }

    private record UsageLine(@NotNull String syntax, @NotNull String description, @NotNull List<String> examples) {
    }

    private static String validateName(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Command " + name + " must not be blank or contain whitespace.");
        }
        return trimmed;
    }

    public static final class Node {
        private final ArgumentBuilder<CommandSourceStack, ?> builder;
        private final String usagePart;
        private final List<Node> children = new ArrayList<>();
        private final List<ArgumentBuilder<CommandSourceStack, ?>> brigadierChildren = new ArrayList<>();
        private final List<String> examples = new ArrayList<>();
        private final List<CommandCooldown> cooldowns = new ArrayList<>();
        private String description = "";
        private boolean executable;
        private boolean childrenAttached;

        private Node(@NotNull ArgumentBuilder<CommandSourceStack, ?> builder, @NotNull String usagePart) {
            this.builder = Objects.requireNonNull(builder, "builder");
            this.usagePart = Objects.requireNonNull(usagePart, "usagePart");
        }

        @NotNull
        public Node literal(@NotNull String name) {
            ensureMutable();
            String literal = validateName(name, "literal");
            Node child = new Node(Commands.literal(literal), literal);
            this.children.add(child);
            return child;
        }

        @NotNull
        public Node literal(@NotNull String name, @NotNull Consumer<Node> configure) {
            Node child = literal(name);
            configure.accept(child);
            return this;
        }

        @NotNull
        public Node thenBrigadier(@NotNull ArgumentBuilder<CommandSourceStack, ?> child) {
            ensureMutable();
            this.brigadierChildren.add(Objects.requireNonNull(child, "child"));
            return this;
        }

        @NotNull
        public <T> Node argument(@NotNull String name, @NotNull ArgumentType<T> type) {
            ensureMutable();
            String argumentName = validateName(name, "argument");
            Node child = new Node(Commands.argument(argumentName, Objects.requireNonNull(type, "type")), "<" + argumentName + ">");
            this.children.add(child);
            return child;
        }

        @NotNull
        public <T> Node argument(@NotNull CommandArgument<T> argument) {
            Objects.requireNonNull(argument, "argument");
            return argument(argument.name(), argument.type());
        }

        @NotNull
        public <T, R extends io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver<T>> Node argument(
                @NotNull ResolvedCommandArgument<T, R> argument
        ) {
            Objects.requireNonNull(argument, "argument");
            return argument(argument.name(), argument.type());
        }

        @NotNull
        public <T> Node argument(@NotNull String name, @NotNull ArgumentType<T> type, @NotNull Consumer<Node> configure) {
            Node child = argument(name, type);
            configure.accept(child);
            return this;
        }

        @NotNull
        public <T> Node argument(@NotNull CommandArgument<T> argument, @NotNull Consumer<Node> configure) {
            Node child = argument(argument);
            configure.accept(child);
            return this;
        }

        @NotNull
        public <T, R extends io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver<T>> Node argument(
                @NotNull ResolvedCommandArgument<T, R> argument,
                @NotNull Consumer<Node> configure
        ) {
            Node child = argument(argument);
            configure.accept(child);
            return this;
        }

        @NotNull
        public Node permission(@NotNull String permission) {
            Objects.requireNonNull(permission, "permission");
            return requires(source -> source.getSender().hasPermission(permission));
        }

        @NotNull
        public Node permission(@NotNull PermissionNode permission) {
            Objects.requireNonNull(permission, "permission");
            return requires(source -> source.getSender().hasPermission(permission.value()));
        }

        @NotNull
        public Node permissions(@NotNull PermissionRequirement requirement) {
            Objects.requireNonNull(requirement, "requirement");
            return requires(requirement.asCommandRequirement());
        }

        @NotNull
        public Node playerOnly() {
            return requires(source -> source.getSender() instanceof Player);
        }

        @NotNull
        public Node description(@NotNull String description) {
            ensureMutable();
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        @NotNull
        public Node example(@NotNull String example) {
            ensureMutable();
            String trimmed = Objects.requireNonNull(example, "example").trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Command example must not be blank.");
            }
            this.examples.add(trimmed.startsWith("/") ? trimmed : "/" + trimmed);
            return this;
        }

        @NotNull
        public Node examples(@NotNull String... examples) {
            Arrays.stream(examples).forEach(this::example);
            return this;
        }

        @NotNull
        public Node examples(@NotNull Collection<String> examples) {
            Objects.requireNonNull(examples, "examples").forEach(this::example);
            return this;
        }

        @NotNull
        public Node requires(@NotNull Predicate<CommandSourceStack> requirement) {
            ensureMutable();
            Objects.requireNonNull(requirement, "requirement");
            Predicate<CommandSourceStack> previous = this.builder.getRequirement();
            this.builder.requires(previous.and(requirement));
            return this;
        }

        @NotNull
        public Node requiresContext(@NotNull Predicate<CommandContext> requirement) {
            Objects.requireNonNull(requirement, "requirement");
            return requires(source -> requirement.test(new CommandContext(source)));
        }

        @NotNull
        public Node cooldown(@NotNull Cooldown cooldown) {
            return cooldown(CommandCooldown.perSender(cooldown));
        }

        @NotNull
        public Node cooldown(@NotNull CommandCooldown cooldown) {
            ensureMutable();
            this.cooldowns.add(Objects.requireNonNull(cooldown, "cooldown"));
            return this;
        }

        @NotNull
        public Node executes(@NotNull CommandExecutor executor) {
            ensureMutable();
            Objects.requireNonNull(executor, "executor");
            this.executable = true;
            this.builder.executes(context -> {
                CommandContext commandContext = new CommandContext(context);
                return execute(commandContext, executor);
            });
            return this;
        }

        /**
         * Executes this command on RamCore's async scheduler.
         *
         * <p>Only use this for work that is safe off the server thread. Schedule
         * back to the appropriate sync, entity, or region scheduler before reading
         * or mutating Bukkit state that is not async-safe.</p>
         */
        @NotNull
        public Node executesAsync(@NotNull CommandExecutor executor) {
            ensureMutable();
            Objects.requireNonNull(executor, "executor");
            this.executable = true;
            this.builder.executes(context -> {
                CommandContext commandContext = new CommandContext(context);
                try {
                    runGuards(commandContext);
                } catch (CommandInterruptException e) {
                    e.getAction().accept(commandContext.sender());
                    return 0;
                }

                Schedulers.async().execute(() -> executeWithoutGuards(commandContext, executor));
                return Command.SINGLE_SUCCESS;
            });
            return this;
        }

        @NotNull
        public Node suggests(@NotNull Collection<String> suggestions) {
            Objects.requireNonNull(suggestions, "suggestions");
            return suggests(CommandSuggestions.of(suggestions));
        }

        @NotNull
        public Node suggests(@NotNull CommandSuggestionProvider provider) {
            ensureMutable();
            Objects.requireNonNull(provider, "provider");
            if (!(this.builder instanceof RequiredArgumentBuilder<?, ?> required)) {
                throw new IllegalStateException("Suggestions can only be attached to argument nodes.");
            }

            RequiredArgumentBuilder<CommandSourceStack, ?> argument = (RequiredArgumentBuilder<CommandSourceStack, ?>) required;
            argument.suggests(toBrigadierSuggestions(provider));
            return this;
        }

        @NotNull
        public ArgumentBuilder<CommandSourceStack, ?> brigadier() {
            return this.builder;
        }

        @NotNull
        public String usagePart() {
            return this.usagePart;
        }

        @NotNull
        public List<Node> children() {
            return List.copyOf(this.children);
        }

        @NotNull
        public String description() {
            return this.description;
        }

        public boolean executable() {
            return this.executable;
        }

        @NotNull
        public List<String> examples() {
            return List.copyOf(this.examples);
        }

        private int execute(CommandContext commandContext, CommandExecutor executor) {
            try {
                runGuards(commandContext);
                executor.execute(commandContext);
                return Command.SINGLE_SUCCESS;
            } catch (CommandInterruptException e) {
                e.getAction().accept(commandContext.sender());
                return 0;
            } catch (Throwable t) {
                LoaderUtils.getPlugin().getLogger().log(Level.SEVERE, "Unhandled exception while executing command", t);
                commandContext.msg("<red>An error occurred while processing this command.");
                return 0;
            }
        }

        private static int executeWithoutGuards(CommandContext commandContext, CommandExecutor executor) {
            try {
                executor.execute(commandContext);
                return Command.SINGLE_SUCCESS;
            } catch (CommandInterruptException e) {
                e.getAction().accept(commandContext.sender());
                return 0;
            } catch (Throwable t) {
                LoaderUtils.getPlugin().getLogger().log(Level.SEVERE, "Unhandled exception while executing command", t);
                commandContext.msg("<red>An error occurred while processing this command.");
                return 0;
            }
        }

        private void runGuards(CommandContext commandContext) throws CommandInterruptException {
            for (CommandCooldown cooldown : this.cooldowns) {
                cooldown.check(commandContext);
            }
        }

        private static SuggestionProvider<CommandSourceStack> toBrigadierSuggestions(CommandSuggestionProvider provider) {
            return (context, builder) -> {
                CommandContext commandContext = new CommandContext(context);
                String remaining = builder.getRemainingLowerCase();
                try {
                    for (CommandSuggestion suggestion : provider.suggest(commandContext, remaining)) {
                        if (suggestion.value().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                            if (suggestion.tooltip() == null) {
                                builder.suggest(suggestion.value());
                            } else {
                                builder.suggest(suggestion.value(), suggestion::tooltip);
                            }
                        }
                    }
                } catch (CommandInterruptException e) {
                    e.getAction().accept(commandContext.sender());
                }
                return builder.buildFuture();
            };
        }

        private void collectUsageStrings(@NotNull String prefix, @NotNull List<String> lines) {
            String current = prefix.isBlank() ? this.usagePart : prefix + " " + this.usagePart;
            if (this.executable) {
                if (this.description.isBlank()) {
                    lines.add(current);
                } else {
                    lines.add(current + " - " + this.description);
                }
            }

            for (Node child : this.children) {
                child.collectUsageStrings(current, lines);
            }
        }

        private void collectUsageLines(@NotNull String prefix, @NotNull List<UsageLine> lines) {
            String current = prefix.isBlank() ? this.usagePart : prefix + " " + this.usagePart;
            if (this.executable) {
                lines.add(new UsageLine(current, this.description, examples()));
            }

            for (Node child : this.children) {
                child.collectUsageLines(current, lines);
            }
        }

        private void collectVisibleUsageLines(
                @NotNull String prefix,
                @NotNull CommandSourceStack source,
                @NotNull List<UsageLine> lines
        ) {
            if (!this.builder.getRequirement().test(source)) {
                return;
            }

            String current = prefix.isBlank() ? this.usagePart : prefix + " " + this.usagePart;
            if (this.executable) {
                lines.add(new UsageLine(current, this.description, examples()));
            }

            for (Node child : this.children) {
                child.collectVisibleUsageLines(current, source, lines);
            }
        }

        private CommandNode<CommandSourceStack> buildNode() {
            attachChildren();
            return this.builder.build();
        }

        private void attachChildren() {
            if (this.childrenAttached) {
                return;
            }

            this.childrenAttached = true;
            attachIncompleteCommandFallback();
            for (Node child : this.children) {
                this.builder.then(child.buildNode());
            }
            for (ArgumentBuilder<CommandSourceStack, ?> child : this.brigadierChildren) {
                this.builder.then(child);
            }
        }

        private void ensureMutable() {
            if (this.childrenAttached) {
                throw new IllegalStateException("Command nodes cannot be modified after the command has been built.");
            }
        }

        private void attachIncompleteCommandFallback() {
            if (this.executable || (this.children.isEmpty() && this.brigadierChildren.isEmpty())) {
                return;
            }

            this.builder.executes(context -> {
                CommandContext commandContext = new CommandContext(context);
                List<String> expected = this.children.stream()
                        .filter(child -> child.builder.getRequirement().test(context.getSource()))
                        .map(Node::usagePart)
                        .toList();

                if (expected.isEmpty()) {
                    commandContext.msg("<red>Incomplete command.");
                } else if (expected.size() == 1) {
                    Node expectedChild = this.children.stream()
                            .filter(child -> child.builder.getRequirement().test(context.getSource()))
                            .findFirst()
                            .orElse(null);
                    String part = expected.getFirst();
                    if (part.startsWith("<") && part.endsWith(">")) {
                        commandContext.msg("<red>Missing required argument: <white>" + part + "</white>");
                    } else {
                        commandContext.msg("<red>Missing required subcommand: <white>" + part + "</white>");
                    }
                    if (expectedChild != null && !expectedChild.examples.isEmpty()) {
                        commandContext.msg("<gray>Example: <white>" + expectedChild.examples.getFirst() + "</white>");
                    }
                } else {
                    commandContext.msg("<red>Missing required subcommand or argument. Expected one of: <white>"
                            + String.join("</white>, <white>", expected)
                            + "</white>");
                }

                return 0;
            });
        }
    }
}
