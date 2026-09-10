package dev.willram.ramcore.utils;

import dev.willram.ramcore.RamPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging facade that works with or without a bound RamCore plugin.
 *
 * <p>On a server, lines go through {@link RamPlugin#log(String)} (MiniMessage-formatted console
 * output) or the plugin logger. Off-server, for example in unit tests, MiniMessage tags are stripped
 * and lines go to a plain {@link Logger} named {@code RamCore}. This keeps scheduler and promise
 * exception reporting usable without Bukkit.</p>
 */
public final class RamLog {
    private static final Logger FALLBACK = Logger.getLogger("RamCore");

    private RamLog() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Checks whether a RamCore plugin is bound and server logging is available.
     *
     * @return true when running inside a server with RamCore loaded
     */
    public static boolean pluginBound() {
        return LoaderUtils.pluginIfBound().isPresent();
    }

    /**
     * Logs an informational MiniMessage line.
     *
     * @param message the MiniMessage-formatted message
     */
    public static void info(@NotNull String message) {
        Objects.requireNonNull(message, "message");
        Optional<RamPlugin> plugin = LoaderUtils.pluginIfBound();
        if (plugin.isPresent()) {
            plugin.get().log(message);
            return;
        }
        FALLBACK.info(strip(message));
    }

    /**
     * Logs a warning.
     *
     * @param message the MiniMessage-formatted message
     */
    public static void warn(@NotNull String message) {
        warn(message, null);
    }

    /**
     * Logs a warning with an optional cause.
     *
     * @param message the MiniMessage-formatted message
     * @param cause   the cause, may be null
     */
    public static void warn(@NotNull String message, @Nullable Throwable cause) {
        log(Level.WARNING, message, cause);
    }

    /**
     * Logs a severe problem with an optional cause.
     *
     * @param message the MiniMessage-formatted message
     * @param cause   the cause, may be null
     */
    public static void severe(@NotNull String message, @Nullable Throwable cause) {
        log(Level.SEVERE, message, cause);
    }

    private static void log(Level level, String message, @Nullable Throwable cause) {
        Objects.requireNonNull(message, "message");
        String plain = strip(message);
        Optional<RamPlugin> plugin = LoaderUtils.pluginIfBound();
        Logger logger = plugin.map(RamPlugin::getLogger).orElse(FALLBACK);
        if (cause == null) {
            logger.log(level, plain);
        } else {
            logger.log(level, plain, cause);
        }
    }

    private static String strip(String message) {
        return MiniMessage.miniMessage().stripTags(message);
    }
}
