package dev.willram.ramcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public final class CommandSuggestions {

    @NotNull
    public static CommandSuggestionProvider of(@NotNull Collection<String> values) {
        Objects.requireNonNull(values, "values");
        return (context, remaining) -> values.stream()
                .map(CommandSuggestion::of)
                .toList();
    }

    @NotNull
    public static CommandSuggestionProvider of(@NotNull String... values) {
        return of(Arrays.asList(values));
    }

    @NotNull
    public static CommandSuggestionProvider dynamic(@NotNull Supplier<Collection<String>> values) {
        Objects.requireNonNull(values, "values");
        return (context, remaining) -> values.get().stream()
                .map(CommandSuggestion::of)
                .toList();
    }

    @NotNull
    public static CommandSuggestionProvider strings(@NotNull StringCommandSuggestionProvider provider) {
        Objects.requireNonNull(provider, "provider");
        return (context, remaining) -> provider.suggest(context, remaining).stream()
                .map(CommandSuggestion::of)
                .toList();
    }

    @NotNull
    public static CommandSuggestionProvider none() {
        return of(List.of());
    }

    @NotNull
    public static CommandSuggestionProvider ofSuggestions(@NotNull Collection<CommandSuggestion> values) {
        Objects.requireNonNull(values, "values");
        return (context, remaining) -> values;
    }

    @NotNull
    public static CommandSuggestionProvider of(@NotNull CommandSuggestion... values) {
        return ofSuggestions(Arrays.asList(values));
    }

    @NotNull
    public static CommandSuggestionProvider dynamicSuggestions(@NotNull Supplier<Collection<CommandSuggestion>> values) {
        Objects.requireNonNull(values, "values");
        return (context, remaining) -> values.get();
    }

    @NotNull
    public static CommandSuggestion suggestion(@NotNull String value) {
        return CommandSuggestion.of(value);
    }

    @NotNull
    public static CommandSuggestion suggestion(@NotNull String value, @NotNull String tooltip) {
        return CommandSuggestion.withTooltip(value, tooltip);
    }

    @NotNull
    public static CommandSuggestionProvider onlinePlayers() {
        return (context, remaining) -> Bukkit.getOnlinePlayers().stream()
                .map(player -> CommandSuggestion.of(player.getName()))
                .toList();
    }

    @NotNull
    public static CommandSuggestionProvider onlinePlayersWithWorlds() {
        return (context, remaining) -> Bukkit.getOnlinePlayers().stream()
                .map(player -> CommandSuggestion.withTooltip(player.getName(), player.getWorld().getName()))
                .toList();
    }

    @NotNull
    public static CommandSuggestionProvider worlds() {
        return (context, remaining) -> Bukkit.getWorlds().stream()
                .map(world -> CommandSuggestion.of(world.getName()))
                .toList();
    }

    @NotNull
    public static CommandSuggestionProvider worldsWithEnvironments() {
        return (context, remaining) -> Bukkit.getWorlds().stream()
                .map(world -> CommandSuggestion.withTooltip(world.getName(), world.getEnvironment().name()))
                .toList();
    }

    @NotNull
    public static Collection<String> filter(@NotNull Collection<String> values, @NotNull String remaining) {
        Objects.requireNonNull(values, "values");
        return values.stream()
                .filter(value -> startsWith(value, remaining))
                .toList();
    }

    @NotNull
    public static Collection<CommandSuggestion> filterSuggestions(@NotNull Collection<CommandSuggestion> values, @NotNull String remaining) {
        Objects.requireNonNull(values, "values");
        return values.stream()
                .filter(value -> startsWith(value.value(), remaining))
                .toList();
    }

    public static boolean startsWith(@NotNull String value, @NotNull String remaining) {
        return value.toLowerCase(Locale.ROOT).startsWith(remaining.toLowerCase(Locale.ROOT));
    }

    private CommandSuggestions() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
