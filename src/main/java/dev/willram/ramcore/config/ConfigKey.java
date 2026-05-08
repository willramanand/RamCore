package dev.willram.ramcore.config;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Typed path definition for {@link BukkitConfig}.
 */
public final class ConfigKey<T> {
    private final String path;
    private final Class<T> type;
    private final T defaultValue;
    private final boolean required;
    private final List<Rule<T>> rules;

    private ConfigKey(
            @NotNull String path,
            @NotNull Class<T> type,
            @Nullable T defaultValue,
            boolean required,
            @NotNull List<Rule<T>> rules
    ) {
        this.path = requireNonNull(path, "path");
        this.type = requireNonNull(type, "type");
        this.defaultValue = defaultValue;
        this.required = required;
        this.rules = List.copyOf(rules);

        RamPreconditions.checkArgument(!path.isBlank(), "config path must not be blank", "Use a dotted YAML path such as 'messages.prefix'.");
    }

    @NotNull
    public static <T> ConfigKey<T> of(@NotNull String path, @NotNull Class<T> type, @NotNull T defaultValue) {
        return new ConfigKey<>(path, type, requireNonNull(defaultValue, "defaultValue"), false, List.of());
    }

    @NotNull
    public static <T> ConfigKey<T> required(@NotNull String path, @NotNull Class<T> type) {
        return new ConfigKey<>(path, type, null, true, List.of());
    }

    @NotNull
    public ConfigKey<T> validate(@NotNull Predicate<? super T> predicate, @NotNull String message) {
        requireNonNull(predicate, "predicate");
        requireNonNull(message, "message");

        List<Rule<T>> next = new ArrayList<>(this.rules);
        next.add(new Rule<>(predicate, message));
        return new ConfigKey<>(this.path, this.type, this.defaultValue, this.required, next);
    }

    @NotNull
    public String path() {
        return this.path;
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @Nullable
    public T defaultValue() {
        return this.defaultValue;
    }

    public boolean required() {
        return this.required;
    }

    @NotNull
    List<String> validateValue(@NotNull T value) {
        List<String> errors = new ArrayList<>();
        for (Rule<T> rule : this.rules) {
            if (!rule.predicate().test(value)) {
                errors.add(this.path + ": " + rule.message());
            }
        }
        return errors;
    }

    private record Rule<T>(Predicate<? super T> predicate, String message) {
    }
}
