package dev.willram.ramcore.commands;

import dev.willram.ramcore.cooldown.Cooldown;
import dev.willram.ramcore.cooldown.CooldownMap;
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
    private final CooldownMap<Object> cooldowns;
    private final Function<CommandContext, ?> keyFunction;
    private final BiFunction<CommandContext, Long, String> messageFunction;

    private CommandCooldown(
            @NotNull Cooldown cooldown,
            @NotNull Function<CommandContext, ?> keyFunction,
            @NotNull BiFunction<CommandContext, Long, String> messageFunction
    ) {
        this.cooldowns = CooldownMap.create(Objects.requireNonNull(cooldown, "cooldown"));
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        this.messageFunction = Objects.requireNonNull(messageFunction, "messageFunction");
    }

    @NotNull
    public static CommandCooldown perSender(@NotNull Cooldown cooldown) {
        return keyed(cooldown, context -> senderKey(context.sender()));
    }

    @NotNull
    public static CommandCooldown keyed(@NotNull Cooldown cooldown, @NotNull Function<CommandContext, ?> keyFunction) {
        return new CommandCooldown(cooldown, keyFunction, CommandCooldown::defaultMessage);
    }

    @NotNull
    public CommandCooldown message(@NotNull BiFunction<CommandContext, Long, String> messageFunction) {
        return new CommandCooldown(this.cooldowns.getBase(), this.keyFunction, messageFunction);
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

        if (this.cooldowns.test(key)) {
            return;
        }

        long remainingMillis = this.cooldowns.remainingMillis(key);
        throw context.fail(this.messageFunction.apply(context, remainingMillis));
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
}
