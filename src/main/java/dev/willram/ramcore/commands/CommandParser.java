package dev.willram.ramcore.commands;

import dev.willram.ramcore.commands.arguments.ArgumentParserRegistry;
import dev.willram.ramcore.commands.arguments.SimpleParserRegistry;
import dev.willram.ramcore.functions.Numbers;
import dev.willram.ramcore.time.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public final class CommandParser {

    // Global argument parsers
    private static final ArgumentParserRegistry PARSER_REGISTRY;

    @Nonnull
    public static ArgumentParserRegistry parserRegistry() {
        return PARSER_REGISTRY;
    }

    static {
        PARSER_REGISTRY = new SimpleParserRegistry();

        // setup default argument parsers
        PARSER_REGISTRY.register(String.class, Optional::of);
        PARSER_REGISTRY.register(Number.class, Numbers::parse);
        PARSER_REGISTRY.register(Integer.class, Numbers::parseIntegerOpt);
        PARSER_REGISTRY.register(Long.class, Numbers::parseLongOpt);
        PARSER_REGISTRY.register(Float.class, Numbers::parseFloatOpt);
        PARSER_REGISTRY.register(Double.class, Numbers::parseDoubleOpt);
        PARSER_REGISTRY.register(Byte.class, Numbers::parseByteOpt);
        PARSER_REGISTRY.register(Boolean.class, s -> s.equalsIgnoreCase("true") ? Optional.of(true) : s.equalsIgnoreCase("false") ? Optional.of(false) : Optional.empty());
        PARSER_REGISTRY.register(UUID.class, s -> {
            try {
                return Optional.of(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        });
        PARSER_REGISTRY.register(Player.class, s -> {
            try {
                return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(s)));
            } catch (IllegalArgumentException e) {
                return Optional.ofNullable(Bukkit.getPlayerExact(s));
            }
        });
        PARSER_REGISTRY.register(OfflinePlayer.class, s -> {
            try {
                return Optional.of(Bukkit.getOfflinePlayer(UUID.fromString(s)));
            } catch (IllegalArgumentException e) {
                return Optional.of(Bukkit.getOfflinePlayer(s));
            }
        });
        PARSER_REGISTRY.register(World.class, s -> Optional.ofNullable(Bukkit.getWorld(s)));
        PARSER_REGISTRY.register(Duration.class, DurationParser::parseSafely);
    }

    private CommandParser() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
