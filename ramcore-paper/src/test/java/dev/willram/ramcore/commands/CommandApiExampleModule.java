package dev.willram.ramcore.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
final class CommandApiExampleModule implements CommandModule {
    private static final ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> TARGET = RamArguments.player("target");
    private static final CommandArgument<Integer> AMOUNT = RamArguments.integer("amount", 1, 64);
    private static final CommandArgument<GameMode> MODE = RamArguments.gameMode("mode");
    private static final CommandArgument<String> WORLD = RamArguments.word("world");
    private static final CommandArgument<String> PLAYER_NAME = RamArguments.word("player");

    private final CommandSpec command = createCommand();

    @Override
    public @NotNull Collection<CommandSpec> commands() {
        return List.of(this.command);
    }

    private static CommandSpec createCommand() {
        CommandSpec spec = RamCommands.command("ramcoreexample")
                .description("Example command tree for RamCore command API consumers.")
                .alias("rce")
                .withHelp();

        spec.executes(context -> context.usage(spec));

        spec.literal("give", give -> give
                .permission("ramcore.example.give")
                .argument(TARGET, target -> target
                        .argument(AMOUNT, amount -> amount
                                .description("Give a selected player an example amount.")
                                .executes(context -> {
                                    Player selectedPlayer = context.player(TARGET);
                                    int selectedAmount = context.get(AMOUNT);
                                    context.reply("<green>Would give <white>" + selectedAmount + "</white> item(s) to <white>" + selectedPlayer.getName() + "</white>.");
                                }))));

        spec.literal("mode", mode -> mode
                .playerOnly()
                .argument(MODE, argument -> argument
                        .suggests(CommandSuggestions.of(
                                CommandSuggestions.suggestion("survival", "Standard survival mode"),
                                CommandSuggestions.suggestion("creative", "Unlimited building mode"),
                                CommandSuggestions.suggestion("adventure", "Map and quest mode"),
                                CommandSuggestions.suggestion("spectator", "Observe without interaction")
                        ))
                        .description("Change your own game mode.")
                        .executes(context -> {
                            Player player = context.requirePlayer();
                            GameMode gameMode = context.get(MODE);
                            player.setGameMode(gameMode);
                            context.reply("<green>Set your game mode to <white>" + gameMode.name().toLowerCase() + "</white>.");
                        })));

        spec.literal("world", world -> world
                .argument(WORLD, argument -> argument
                        .suggests(CommandSuggestions.worldsWithEnvironments())
                        .description("Show information about a world.")
                        .executes(context -> {
                            String worldName = context.get(WORLD);
                            World selectedWorld = Bukkit.getWorld(worldName);
                            context.assertTrue(selectedWorld != null, "<red>Unknown world: <white>" + worldName + "</white>");
                            context.reply("<green>" + selectedWorld.getName() + " has <white>" + selectedWorld.getPlayers().size() + "</white> player(s).");
                        })));

        spec.literal("lookup", lookup -> lookup
                .argument(PLAYER_NAME, argument -> argument
                        .suggests(CommandSuggestions.onlinePlayersWithWorlds())
                        .description("Run an async lookup using a tab-completed player name.")
                        .executesAsync(context -> {
                            String name = context.get(PLAYER_NAME);
                            context.reply("<gray>Would perform async lookup for <white>" + name + "</white>.");
                        })));

        spec.literal("state", state -> state
                .argument(RamArguments.word("value"), argument -> argument
                        .suggests(CommandSuggestions.strings((context, remaining) -> List.of(
                                "sender:" + context.sender().getName(),
                                "input:" + context.input()
                        )))
                        .description("Demonstrate context-aware string suggestions.")
                        .executes(context -> context.reply("<green>State accepted."))));

        spec.root().thenBrigadier(Commands.literal("raw")
                .executes(context -> {
                    context.getSource().getSender().sendRichMessage("<green>Executed a raw Brigadier node.");
                    return Command.SINGLE_SUCCESS;
                }));

        return spec;
    }
}
