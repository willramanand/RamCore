package dev.willram.ramcore.commands;

import dev.willram.ramcore.cooldown.Cooldown;
import dev.willram.ramcore.cooldown.CooldownKey;
import dev.willram.ramcore.cooldown.CooldownResult;
import dev.willram.ramcore.cooldown.CooldownTracker;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.time.DurationFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Per-key command cooldown guard.
 */
public final class CommandCooldown {
    private final CooldownTracker<Object> cooldowns;
    private final KeyFunction keyFunction;
    private final BiFunction<CommandContext, Long, String> messageFunction;

    private CommandCooldown(
            @NotNull Cooldown cooldown,
            @NotNull KeyFunction keyFunction,
            @NotNull BiFunction<CommandContext, Long, String> messageFunction
    ) {
        this(CooldownTracker.create(Objects.requireNonNull(cooldown, "cooldown")), keyFunction, messageFunction);
    }

    private CommandCooldown(
            @NotNull CooldownTracker<Object> cooldowns,
            @NotNull KeyFunction keyFunction,
            @NotNull BiFunction<CommandContext, Long, String> messageFunction
    ) {
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        this.messageFunction = Objects.requireNonNull(messageFunction, "messageFunction");
    }

    @NotNull
    public static CommandCooldown perSender(@NotNull Cooldown cooldown) {
        return keyed(cooldown, context -> senderKey(context.sender()));
    }

    @NotNull
    public static CommandCooldown perPlayer(@NotNull Cooldown cooldown) {
        return keyedInterrupting(cooldown, context -> context.requirePlayer().getUniqueId());
    }

    @NotNull
    public static CommandCooldown keyed(@NotNull Cooldown cooldown, @NotNull Function<CommandContext, ?> keyFunction) {
        Objects.requireNonNull(keyFunction, "keyFunction");
        return keyedInterrupting(cooldown, keyFunction::apply);
    }

    @NotNull
    public static CommandCooldown grouped(@NotNull Cooldown cooldown, @NotNull String group, @NotNull Function<CommandContext, ?> keyFunction) {
        Objects.requireNonNull(keyFunction, "keyFunction");
        return keyed(cooldown, context -> CooldownKey.of(group, keyFunction.apply(context)));
    }

    @NotNull
    public static CommandCooldown keyedInterrupting(@NotNull Cooldown cooldown, @NotNull KeyFunction keyFunction) {
        return new CommandCooldown(cooldown, keyFunction, CommandCooldown::defaultMessage);
    }

    @NotNull
    public CommandCooldown message(@NotNull BiFunction<CommandContext, Long, String> messageFunction) {
        return new CommandCooldown(this.cooldowns, this.keyFunction, messageFunction);
    }

    @NotNull
    public CooldownTracker<Object> tracker() {
        return this.cooldowns;
    }

    public void check(@NotNull CommandContext context) throws CommandInterruptException {
        Objects.requireNonNull(context, "context");
        Object key = this.keyFunction.apply(context);
        if (key == null) {
            throw RamPreconditions.misuse(
                    "command cooldown key function returned null",
                    "Return a stable non-null key, such as player UUID, sender name, or a command-specific string."
            );
        }

        CooldownResult<Object> result = this.cooldowns.test(key);
        if (result.allowed()) {
            return;
        }

        throw context.fail(this.messageFunction.apply(context, result.remainingMillis()));
    }

    private static Object senderKey(CommandSender sender) {
        if (sender instanceof Player player) {
            UUID uuid = player.getUniqueId();
            if (uuid != null) {
                return uuid;
            }
        }

        return sender.getName();
    }

    private static String defaultMessage(CommandContext context, long remainingMillis) {
        String remaining = DurationFormatter.CONCISE_LOW_ACCURACY.format(Duration.ofMillis(Math.max(1000L, remainingMillis)));
        return "<red>Please wait <white>" + remaining + "</white> before using this command again.";
    }

    @FunctionalInterface
    public interface KeyFunction {
        Object apply(@NotNull CommandContext context) throws CommandInterruptException;
    }
}
