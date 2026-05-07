package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.commands.CommandArgument;
import dev.willram.ramcore.commands.CommandContext;
import dev.willram.ramcore.commands.CommandModule;
import dev.willram.ramcore.commands.CommandSpec;
import dev.willram.ramcore.commands.CommandSuggestions;
import dev.willram.ramcore.commands.RamArguments;
import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.commands.ResolvedCommandArgument;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class FoliaDiagnosticsCommandModule implements CommandModule {
    private static final CommandArgument<Integer> TICKS = RamArguments.integer("ticks", 1, 400);
    private static final CommandArgument<Integer> RUNS = RamArguments.integer("runs", 1, 10);
    private static final CommandArgument<String> MESSAGE = RamArguments.greedyString("message");
    private static final ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> PLAYER = RamArguments.player("player");
    private static final ResolvedCommandArgument<List<Entity>, EntitySelectorArgumentResolver> ENTITY = RamArguments.entity("entity");

    private final RamPlugin plugin;
    private final CommandSpec command;

    public FoliaDiagnosticsCommandModule(@NotNull RamPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.command = createCommand();
    }

    @Override
    public @NotNull Collection<CommandSpec> commands() {
        return List.of(this.command);
    }

    private CommandSpec createCommand() {
        CommandSpec spec = RamCommands.command("ramcore")
                .description("RamCore diagnostics for commands, Folia scheduling, and optional integrations.")
                .alias("rcore")
                .permission("ramcore.diagnostics")
                .withHelp();

        spec.executes(context -> context.usage(spec));

        spec.literal("ping", ping -> ping
                .description("Verify command registration and basic execution.")
                .example("ramcore ping")
                .executes(context -> context.reply("<green>RamCore diagnostics command is registered.")));

        spec.literal("echo", echo -> echo
                .argument(MESSAGE, message -> message
                        .description("Verify greedy string argument parsing.")
                        .example("ramcore echo hello from folia")
                        .executes(context -> context.reply("<gray>echo:</gray> <white>" + context.get(MESSAGE) + "</white>"))));

        spec.literal("scheduler", scheduler -> scheduler
                .literal("global", global -> global
                        .description("Run a task on the global scheduler.")
                        .example("ramcore scheduler global")
                        .executes(context -> {
                            CommandSender sender = context.sender();
                            Schedulers.runGlobal(() -> sender.sendRichMessage("<green>Global scheduler OK on <white>" + Thread.currentThread().getName() + "</white>."));
                            context.reply("<gray>Queued global scheduler check.");
                        }))
                .literal("async", async -> async
                        .description("Run async work, then report back on a safe scheduler.")
                        .example("ramcore scheduler async")
                        .executes(context -> {
                            CommandSender sender = context.sender();
                            Player player = context.player();
                            Schedulers.runAsync(() -> respond(sender, player, "<green>Async scheduler OK from <white>" + Thread.currentThread().getName() + "</white>."));
                            context.reply("<gray>Queued async scheduler check.");
                        }))
                .literal("player", player -> player
                        .playerOnly()
                        .description("Run a player-anchored task.")
                        .example("ramcore scheduler player")
                        .executes(context -> {
                            Player self = context.requirePlayer();
                            Schedulers.run(self, () -> self.sendRichMessage("<green>Player scheduler OK at <white>" + formatLocation(self.getLocation()) + "</white>."));
                            context.reply("<gray>Queued player scheduler check.");
                        }))
                .literal("region", region -> region
                        .playerOnly()
                        .description("Run a region-anchored task at your location.")
                        .example("ramcore scheduler region")
                        .executes(context -> {
                            Player player = context.requirePlayer();
                            Location location = player.getLocation();
                            Schedulers.run(location, () -> player.sendRichMessage("<green>Region scheduler OK for <white>" + formatLocation(location) + "</white>."));
                            context.reply("<gray>Queued region scheduler check.");
                        }))
                .literal("delayed", delayed -> delayed
                        .playerOnly()
                        .argument(TICKS, ticksArgument -> ticksArgument
                                .description("Run a delayed player-anchored task.")
                                .example("ramcore scheduler delayed 40")
                                .executes(context -> {
                                    Player player = context.requirePlayer();
                                    int ticks = context.get(TICKS);
                                    Schedulers.runLater(player, () -> player.sendRichMessage("<green>Delayed player task fired after <white>" + ticks + "</white> tick(s)."), ticks, this.plugin);
                                    context.reply("<gray>Queued delayed player task for <white>" + ticks + "</white> tick(s).");
                                })))
                .literal("retired", retired -> retired
                        .playerOnly()
                        .argument(TICKS, ticksArgument -> ticksArgument
                                .description("Run a delayed entity task with a retired callback; log out before it fires to test retirement.")
                                .example("ramcore scheduler retired 200")
                                .executes(context -> {
                                    Player player = context.requirePlayer();
                                    int ticks = context.get(TICKS);
                                    this.plugin.bind(Schedulers.runLater(player,
                                            () -> player.sendRichMessage("<green>Retire-aware task fired while you were still schedulable."),
                                            () -> this.plugin.log("<yellow>Retire-aware diagnostic task was retired for " + player.getName() + "."),
                                            ticks
                                    ));
                                    context.reply("<gray>Queued retire-aware task for <white>" + ticks + "</white> tick(s).");
                                })))
                .literal("repeating", repeating -> repeating
                        .playerOnly()
                        .argument(RUNS, runsArgument -> runsArgument
                                .description("Run a short player-anchored repeating task.")
                                .example("ramcore scheduler repeating 3")
                                .executes(context -> {
                                    Player player = context.requirePlayer();
                                    int runs = context.get(RUNS);
                                    Task task = Schedulers.runTimerTask(player, 0L, 20L, scheduledTask -> {
                                        int currentRun = scheduledTask.getTimesRan() + 1;
                                        player.sendRichMessage("<green>Repeating player task run <white>" + currentRun + "</white>/<white>" + runs + "</white>.");
                                        if (currentRun >= runs) {
                                            scheduledTask.stop();
                                            player.sendRichMessage("<gray>Repeating player task stopped.");
                                        }
                                    });
                                    this.plugin.bind(task);
                                    context.reply("<gray>Queued repeating player task for <white>" + runs + "</white> run(s).");
                                }))));

        spec.literal("selector", selector -> selector
                .literal("player", player -> player
                        .argument(PLAYER, argument -> argument
                                .description("Resolve one Paper player selector.")
                                .example("ramcore selector player @s")
                                .executes(context -> {
                                    Player selected = context.player(PLAYER);
                                    context.reply("<green>Resolved player selector to <white>" + selected.getName() + "</white>.");
                                })))
                .literal("entity", entity -> entity
                        .argument(ENTITY, argument -> argument
                                .description("Resolve one Paper entity selector.")
                                .example("ramcore selector entity @s")
                                .executes(context -> {
                                    Entity selected = context.entity(ENTITY);
                                    context.reply("<green>Resolved entity selector to <white>" + selected.getType().name().toLowerCase(Locale.ROOT) + "</white>.");
                                }))));

        spec.literal("protocol", protocol -> protocol
                .description("Check whether optional ProtocolLib is present.")
                .example("ramcore protocol")
                .executes(context -> {
                    boolean present = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
                    context.reply(present
                            ? "<green>ProtocolLib is installed and visible."
                            : "<yellow>ProtocolLib is not installed; optional path is inactive.");
                }));

        return spec;
    }

    private static void respond(@NotNull CommandSender sender, @Nullable Player player, @NotNull String message) {
        if (player != null && player.isOnline()) {
            Schedulers.run(player, () -> sender.sendRichMessage(message));
        } else {
            Schedulers.runGlobal(() -> sender.sendRichMessage(message));
        }
    }

    private static String formatLocation(@NotNull Location location) {
        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + ","
                + location.getBlockY()
                + ","
                + location.getBlockZ();
    }
}
