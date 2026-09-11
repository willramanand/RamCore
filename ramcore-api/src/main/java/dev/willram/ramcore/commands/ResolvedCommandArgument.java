package dev.willram.ramcore.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.ArgumentResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ResolvedCommandArgument<T, R extends ArgumentResolver<T>> {
    private final String name;
    private final ArgumentType<R> type;
    private final Class<R> resolverType;

    private ResolvedCommandArgument(@NotNull String name, @NotNull ArgumentType<R> type, @NotNull Class<R> resolverType) {
        this.name = CommandArgument.validateName(name);
        this.type = Objects.requireNonNull(type, "type");
        this.resolverType = Objects.requireNonNull(resolverType, "resolverType");
    }

    @NotNull
    public static <T, R extends ArgumentResolver<T>> ResolvedCommandArgument<T, R> of(
            @NotNull String name,
            @NotNull ArgumentType<R> type,
            @NotNull Class<R> resolverType
    ) {
        return new ResolvedCommandArgument<>(name, type, resolverType);
    }

    @NotNull
    public String name() {
        return this.name;
    }

    @NotNull
    public ArgumentType<R> type() {
        return this.type;
    }

    @NotNull
    public Class<R> resolverType() {
        return this.resolverType;
    }
}
