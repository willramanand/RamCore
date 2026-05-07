package dev.willram.ramcore.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class CommandArgument<T> {
    private final String name;
    private final ArgumentType<T> type;
    private final Class<T> valueType;

    private CommandArgument(@NotNull String name, @NotNull ArgumentType<T> type, @NotNull Class<T> valueType) {
        this.name = validateName(name);
        this.type = Objects.requireNonNull(type, "type");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
    }

    @NotNull
    public static <T> CommandArgument<T> of(@NotNull String name, @NotNull ArgumentType<T> type, @NotNull Class<T> valueType) {
        return new CommandArgument<>(name, type, valueType);
    }

    @NotNull
    public String name() {
        return this.name;
    }

    @NotNull
    public ArgumentType<T> type() {
        return this.type;
    }

    @NotNull
    public Class<T> valueType() {
        return this.valueType;
    }

    static String validateName(String value) {
        Objects.requireNonNull(value, "name");
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Command argument name must not be blank or contain whitespace.");
        }
        return trimmed;
    }
}
