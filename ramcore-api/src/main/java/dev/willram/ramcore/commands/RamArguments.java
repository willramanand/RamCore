package dev.willram.ramcore.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class RamArguments {

    @NotNull
    public static <T> CommandArgument<T> custom(
            @NotNull String name,
            @NotNull ArgumentType<T> type,
            @NotNull Class<T> valueType
    ) {
        return CommandArgument.of(name, type, valueType);
    }

    @NotNull
    public static <T, R extends ArgumentResolver<T>> ResolvedCommandArgument<T, R> resolved(
            @NotNull String name,
            @NotNull ArgumentType<R> type,
            @NotNull Class<R> resolverType
    ) {
        return ResolvedCommandArgument.of(name, type, resolverType);
    }

    @NotNull
    public static ArgumentType<String> word() {
        return StringArgumentType.word();
    }

    @NotNull
    public static CommandArgument<String> word(@NotNull String name) {
        return CommandArgument.of(name, word(), String.class);
    }

    @NotNull
    public static ArgumentType<String> string() {
        return StringArgumentType.string();
    }

    @NotNull
    public static CommandArgument<String> string(@NotNull String name) {
        return CommandArgument.of(name, string(), String.class);
    }

    @NotNull
    public static ArgumentType<String> greedyString() {
        return StringArgumentType.greedyString();
    }

    @NotNull
    public static CommandArgument<String> greedyString(@NotNull String name) {
        return CommandArgument.of(name, greedyString(), String.class);
    }

    @NotNull
    public static ArgumentType<Integer> integer() {
        return IntegerArgumentType.integer();
    }

    @NotNull
    public static CommandArgument<Integer> integer(@NotNull String name) {
        return CommandArgument.of(name, integer(), Integer.class);
    }

    @NotNull
    public static ArgumentType<Integer> integer(int min) {
        return IntegerArgumentType.integer(min);
    }

    @NotNull
    public static CommandArgument<Integer> integer(@NotNull String name, int min) {
        return CommandArgument.of(name, integer(min), Integer.class);
    }

    @NotNull
    public static ArgumentType<Integer> integer(int min, int max) {
        return IntegerArgumentType.integer(min, max);
    }

    @NotNull
    public static CommandArgument<Integer> integer(@NotNull String name, int min, int max) {
        return CommandArgument.of(name, integer(min, max), Integer.class);
    }

    @NotNull
    public static ArgumentType<Long> longArg() {
        return LongArgumentType.longArg();
    }

    @NotNull
    public static CommandArgument<Long> longArg(@NotNull String name) {
        return CommandArgument.of(name, longArg(), Long.class);
    }

    @NotNull
    public static ArgumentType<Long> longArg(long min) {
        return LongArgumentType.longArg(min);
    }

    @NotNull
    public static CommandArgument<Long> longArg(@NotNull String name, long min) {
        return CommandArgument.of(name, longArg(min), Long.class);
    }

    @NotNull
    public static ArgumentType<Long> longArg(long min, long max) {
        return LongArgumentType.longArg(min, max);
    }

    @NotNull
    public static CommandArgument<Long> longArg(@NotNull String name, long min, long max) {
        return CommandArgument.of(name, longArg(min, max), Long.class);
    }

    @NotNull
    public static ArgumentType<Float> floatArg() {
        return FloatArgumentType.floatArg();
    }

    @NotNull
    public static CommandArgument<Float> floatArg(@NotNull String name) {
        return CommandArgument.of(name, floatArg(), Float.class);
    }

    @NotNull
    public static ArgumentType<Float> floatArg(float min) {
        return FloatArgumentType.floatArg(min);
    }

    @NotNull
    public static CommandArgument<Float> floatArg(@NotNull String name, float min) {
        return CommandArgument.of(name, floatArg(min), Float.class);
    }

    @NotNull
    public static ArgumentType<Float> floatArg(float min, float max) {
        return FloatArgumentType.floatArg(min, max);
    }

    @NotNull
    public static CommandArgument<Float> floatArg(@NotNull String name, float min, float max) {
        return CommandArgument.of(name, floatArg(min, max), Float.class);
    }

    @NotNull
    public static ArgumentType<Double> doubleArg() {
        return DoubleArgumentType.doubleArg();
    }

    @NotNull
    public static CommandArgument<Double> doubleArg(@NotNull String name) {
        return CommandArgument.of(name, doubleArg(), Double.class);
    }

    @NotNull
    public static ArgumentType<Double> doubleArg(double min) {
        return DoubleArgumentType.doubleArg(min);
    }

    @NotNull
    public static CommandArgument<Double> doubleArg(@NotNull String name, double min) {
        return CommandArgument.of(name, doubleArg(min), Double.class);
    }

    @NotNull
    public static ArgumentType<Double> doubleArg(double min, double max) {
        return DoubleArgumentType.doubleArg(min, max);
    }

    @NotNull
    public static CommandArgument<Double> doubleArg(@NotNull String name, double min, double max) {
        return CommandArgument.of(name, doubleArg(min, max), Double.class);
    }

    @NotNull
    public static ArgumentType<Boolean> bool() {
        return BoolArgumentType.bool();
    }

    @NotNull
    public static CommandArgument<Boolean> bool(@NotNull String name) {
        return CommandArgument.of(name, bool(), Boolean.class);
    }

    @NotNull
    public static ArgumentType<UUID> uuid() {
        return ArgumentTypes.uuid();
    }

    @NotNull
    public static CommandArgument<UUID> uuid(@NotNull String name) {
        return CommandArgument.of(name, uuid(), UUID.class);
    }

    @NotNull
    public static ArgumentType<World> world() {
        return ArgumentTypes.world();
    }

    @NotNull
    public static CommandArgument<World> world(@NotNull String name) {
        return CommandArgument.of(name, world(), World.class);
    }

    @NotNull
    public static ArgumentType<GameMode> gameMode() {
        return ArgumentTypes.gameMode();
    }

    @NotNull
    public static CommandArgument<GameMode> gameMode(@NotNull String name) {
        return CommandArgument.of(name, gameMode(), GameMode.class);
    }

    @NotNull
    public static ArgumentType<Component> component() {
        return ArgumentTypes.component();
    }

    @NotNull
    public static CommandArgument<Component> component(@NotNull String name) {
        return CommandArgument.of(name, component(), Component.class);
    }

    @NotNull
    public static ArgumentType<PlayerSelectorArgumentResolver> player() {
        return ArgumentTypes.player();
    }

    @NotNull
    public static ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> player(@NotNull String name) {
        return ResolvedCommandArgument.of(name, player(), PlayerSelectorArgumentResolver.class);
    }

    @NotNull
    public static ArgumentType<PlayerSelectorArgumentResolver> players() {
        return ArgumentTypes.players();
    }

    @NotNull
    public static ResolvedCommandArgument<List<Player>, PlayerSelectorArgumentResolver> players(@NotNull String name) {
        return ResolvedCommandArgument.of(name, players(), PlayerSelectorArgumentResolver.class);
    }

    @NotNull
    public static ArgumentType<EntitySelectorArgumentResolver> entity() {
        return ArgumentTypes.entity();
    }

    @NotNull
    public static ResolvedCommandArgument<List<Entity>, EntitySelectorArgumentResolver> entity(@NotNull String name) {
        return ResolvedCommandArgument.of(name, entity(), EntitySelectorArgumentResolver.class);
    }

    @NotNull
    public static ArgumentType<EntitySelectorArgumentResolver> entities() {
        return ArgumentTypes.entities();
    }

    @NotNull
    public static ResolvedCommandArgument<List<Entity>, EntitySelectorArgumentResolver> entities(@NotNull String name) {
        return ResolvedCommandArgument.of(name, entities(), EntitySelectorArgumentResolver.class);
    }

    private RamArguments() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
