package dev.willram.ramcore.diagnostics;

import dev.willram.ramcore.ai.MobAi;
import dev.willram.ramcore.ai.MobAiDiagnostics;
import dev.willram.ramcore.brain.MobBrainDiagnostics;
import dev.willram.ramcore.brain.MobBrains;
import dev.willram.ramcore.RamPlugin;
import dev.willram.ramcore.commands.CommandArgument;
import dev.willram.ramcore.commands.CommandContext;
import dev.willram.ramcore.commands.CommandInterruptException;
import dev.willram.ramcore.commands.CommandModule;
import dev.willram.ramcore.commands.CommandSpec;
import dev.willram.ramcore.commands.CommandSuggestions;
import dev.willram.ramcore.commands.RamArguments;
import dev.willram.ramcore.commands.RamCommands;
import dev.willram.ramcore.commands.ResolvedCommandArgument;
import dev.willram.ramcore.integration.IntegrationRegistry;
import dev.willram.ramcore.integration.Integrations;
import dev.willram.ramcore.item.nbt.ItemSnapshot;
import dev.willram.ramcore.nms.api.NmsAccess;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.world.BlockEntitySnapshot;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class FoliaDiagnosticsCommandModule implements CommandModule {
    private static final CommandArgument<Integer> TICKS = RamArguments.integer("ticks", 1, 400);
    private static final CommandArgument<Integer> RUNS = RamArguments.integer("runs", 1, 10);
    private static final CommandArgument<String> MESSAGE = RamArguments.greedyString("message");
    private static final CommandArgument<String> PROVIDER = RamArguments.word("provider");
    private static final ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> PLAYER = RamArguments.player("player");
    private static final ResolvedCommandArgument<List<Entity>, EntitySelectorArgumentResolver> ENTITY = RamArguments.entity("entity");

    private final RamPlugin plugin;
    private final IntegrationRegistry integrations;
    private final NmsAccessRegistry nms;
    private final DiagnosticRegistry diagnosticRegistry;
    private final CommandSpec command;

    public FoliaDiagnosticsCommandModule(@NotNull RamPlugin plugin) {
        this(plugin, Integrations.standard(), NmsAccess.runtimeRegistry(), DiagnosticRegistry.create());
    }

    public FoliaDiagnosticsCommandModule(@NotNull RamPlugin plugin,
                                         @NotNull IntegrationRegistry integrations,
                                         @NotNull NmsAccessRegistry nms,
                                         @NotNull DiagnosticRegistry diagnosticRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.integrations = Objects.requireNonNull(integrations, "integrations");
        this.nms = Objects.requireNonNull(nms, "nms");
        this.diagnosticRegistry = Objects.requireNonNull(diagnosticRegistry, "diagnosticRegistry");
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

        spec.literal("diagnostics", diagnostics -> diagnostics
                .description("Dump runtime diagnostics.")
                .example("ramcore diagnostics summary")
                .literal("summary", summary -> summary
                        .description("Show a concise runtime summary.")
                        .example("ramcore diagnostics summary")
                        .executes(context -> sendLines(context, summaryLines())))
                .literal("export", export -> export
                        .description("Show a paste-safe redacted debug export.")
                        .example("ramcore diagnostics export")
                        .executes(context -> sendPlainLines(context, report().safeLines())))
                .literal("services", services -> services
                        .description("List registered services and dependencies.")
                        .example("ramcore diagnostics services")
                        .executes(context -> sendPlainLines(context, this.plugin.services().diagnostics().stream()
                                .map(service -> service.id() + " state=" + service.state()
                                        + " type=" + service.type()
                                        + " dependsOn=" + service.dependencies())
                                .toList())))
                .literal("commands", commands -> commands
                        .description("Dump registered RamCore command trees known to this module.")
                        .example("ramcore diagnostics commands")
                        .executes(context -> sendPlainLines(context, CommandDiagnostics.dump(commands()))))
                .literal("integrations", integrations -> integrations
                        .description("Dump optional integration capabilities.")
                        .example("ramcore diagnostics integrations")
                        .executes(context -> sendPlainLines(context, PluginDiagnostics.integrationLines(this.integrations))))
                .literal("nms", nms -> nms
                        .description("Dump NMS capability diagnostics.")
                        .example("ramcore diagnostics nms")
                        .executes(context -> sendPlainLines(context, this.nms.diagnostics().lines())))
                .literal("providers", providers -> providers
                        .description("List registered diagnostic providers for module validation and previews.")
                        .example("ramcore diagnostics providers")
                        .executes(context -> sendPlainLines(context, providerList())))
                .literal("provider", provider -> provider
                        .argument(PROVIDER, providerArgument -> providerArgument
                                .description("Run a registered diagnostic provider by id.")
                                .example("ramcore diagnostics provider loot")
                                .executes(context -> {
                                    String id = context.get(PROVIDER);
                                    DiagnosticProvider diagnosticProvider = this.diagnosticRegistry.provider(id)
                                            .orElseThrow(() -> context.fail("<red>Unknown diagnostic provider: <white>" + id + "</white>"));
                                    sendPlainLines(context, diagnosticProvider.lines());
                                }))));

        spec.literal("inspect", inspect -> inspect
                .description("Inspect selected runtime objects.")
                .example("ramcore inspect item")
                .literal("item", item -> item
                        .playerOnly()
                        .description("Inspect the item in your main hand.")
                        .example("ramcore inspect item")
                        .executes(context -> inspectItem(context, context.requirePlayer().getInventory().getItem(EquipmentSlot.HAND))))
                .literal("block", block -> block
                        .playerOnly()
                        .description("Inspect the block you are looking at.")
                        .example("ramcore inspect block")
                        .executes(context -> {
                            Player player = context.requirePlayer();
                            Block target = player.getTargetBlockExact(8);
                            if (target == null) {
                                throw context.fail("<red>No block target within 8 blocks.");
                            }
                            inspectBlock(context, target.getState());
                        }))
                .literal("entity", entity -> entity
                        .argument(ENTITY, argument -> argument
                                .description("Inspect one selected entity.")
                                .example("ramcore inspect entity @e[type=zombie,limit=1]")
                                .executes(context -> inspectEntity(context, context.entity(ENTITY)))))
                .literal("context", schedulerContext -> schedulerContext
                        .playerOnly()
                        .description("Inspect scheduler ownership for your current location.")
                        .example("ramcore inspect context")
                        .executes(context -> {
                            Player player = context.requirePlayer();
                            sendPlainLines(context, List.of(
                                    TaskContext.global().description(),
                                    TaskContext.async().description(),
                                    TaskContext.of(player).description(),
                                    TaskContext.of(player.getLocation()).description(),
                                    TaskContext.of(player.getLocation().getChunk()).description()
                            ));
                        })));

        return spec;
    }

    @NotNull
    private PluginDiagnosticReport report() {
        return PluginDiagnostics.capture(this.plugin, commands(), this.integrations, this.nms, this.diagnosticRegistry);
    }

    @NotNull
    private List<String> summaryLines() {
        PluginDiagnosticReport report = report();
        return List.of(
                "<gold>RamCore diagnostics</gold>",
                "<gray>Plugin:</gray> <white>" + report.pluginName() + " " + report.pluginVersion() + "</white>",
                "<gray>Server:</gray> <white>" + report.serverName() + " " + report.serverVersion() + "</white>",
                "<gray>Scheduler:</gray> <white>" + report.scheduler().mode() + "</white>",
                "<gray>Services:</gray> <white>" + report.services().size() + "</white>",
                "<gray>Commands:</gray> <white>" + report.commandTree().size() + " line(s)</white>",
                "<gray>Integrations:</gray> <white>" + report.integrations().size() + "</white>",
                "<gray>NMS checks:</gray> <white>" + report.nms().size() + "</white>",
                "<gray>Providers:</gray> <white>" + this.diagnosticRegistry.providers().size() + "</white>"
        );
    }

    @NotNull
    private List<String> providerList() {
        if (this.diagnosticRegistry.providers().isEmpty()) {
            return List.of("No diagnostic providers registered.");
        }
        return this.diagnosticRegistry.providers().stream()
                .map(provider -> provider.category() + "." + provider.id() + " - " + provider.description())
                .toList();
    }

    private static void inspectItem(@NotNull CommandContext context, @Nullable ItemStack item) throws CommandInterruptException {
        if (item == null || item.getType() == Material.AIR) {
            throw context.fail("<red>No item in main hand.");
        }
        ItemSnapshot snapshot = ItemSnapshot.capture(item);
        List<String> lines = new ArrayList<>();
        lines.add("item.type=" + snapshot.type());
        lines.add("item.amount=" + snapshot.amount());
        lines.add("item.pdc=" + snapshot.pdc());
        lines.add("item.enchantments=" + snapshot.enchantments());
        lines.add("item.attributes=" + snapshot.attributes());
        lines.add("item.components.values=" + snapshot.components().values().keySet());
        lines.add("item.components.markers=" + snapshot.components().markers());
        lines.add("item.components.overridden=" + snapshot.components().overridden());
        lines.add("item.rawNbt=" + snapshot.rawNbt().orElse("<unsupported>"));
        sendPlainLines(context, lines);
    }

    private static void inspectBlock(@NotNull CommandContext context, @NotNull BlockState state) {
        BlockEntitySnapshot snapshot = BlockEntitySnapshot.capture(state);
        sendPlainLines(context, List.of(
                "block.position=" + snapshot.position(),
                "block.material=" + snapshot.material(),
                "block.kind=" + snapshot.kind(),
                "block.data=" + snapshot.blockData(),
                "block.pdc=" + snapshot.pdcKeys(),
                "block.properties=" + snapshot.properties(),
                "block.rawNbt=" + snapshot.rawNbt().orElse("<unsupported>")
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void inspectEntity(@NotNull CommandContext context, @NotNull Entity entity) {
        List<String> lines = new ArrayList<>();
        lines.add("entity.uuid=" + entity.getUniqueId());
        lines.add("entity.type=" + entity.getType());
        lines.add("entity.location=" + formatLocation(entity.getLocation()));
        lines.add("entity.schedulerContext=" + TaskContext.of(entity).description());
        if (entity instanceof PersistentDataHolder holder) {
            Set<String> keys = holder.getPersistentDataContainer().getKeys().stream()
                    .map(NamespacedKey::asString)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            lines.add("entity.pdc=" + keys);
        }
        if (entity instanceof Mob mob) {
            try {
                MobAiDiagnostics diagnostics = MobAi.controller(mob).diagnostics();
                lines.add("mob.goals.all=" + diagnostics.allGoals());
                lines.add("mob.goals.running=" + diagnostics.runningGoals());
                lines.add("mob.goals.tracked=" + diagnostics.trackedGoals());
                lines.add("mob.goals.conflicts=" + diagnostics.conflicts());
                lines.add("mob.target=" + diagnostics.targetId().map(Object::toString).orElse("none"));
            } catch (RuntimeException e) {
                lines.add("mob.goals.error=" + e.getMessage());
            }
        }
        if (entity instanceof LivingEntity living) {
            try {
                MobBrainDiagnostics brain = MobBrains.controller(living).diagnostics(MobBrains.COMMON_DEBUG_KEYS);
                lines.addAll(brain.lines().stream().map(line -> "brain." + line).toList());
            } catch (RuntimeException e) {
                lines.add("brain.error=" + e.getMessage());
            }
        }
        sendPlainLines(context, lines);
    }

    private static void sendLines(@NotNull CommandContext context, @NotNull List<String> lines) {
        if (lines.isEmpty()) {
            context.reply("<gray>No diagnostics available.");
            return;
        }
        for (String line : lines) {
            context.reply(line);
        }
    }

    private static void sendPlainLines(@NotNull CommandContext context, @NotNull List<String> lines) {
        if (lines.isEmpty()) {
            context.msg(net.kyori.adventure.text.Component.text("No diagnostics available."));
            return;
        }
        for (String line : lines) {
            context.msg(net.kyori.adventure.text.Component.text(line));
        }
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
