package dev.willram.ramcore.text;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Builders for common typed text contexts.
 */
public final class TextContexts {

    @NotNull
    public static TextContext player(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return TextContext.builder()
                .put(TextPlaceholders.PLAYER, player)
                .put(TextPlaceholders.PLAYER_UUID, player)
                .build();
    }

    @NotNull
    public static TextContext entity(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return TextContext.builder()
                .put(TextPlaceholders.ENTITY, entity)
                .put(TextPlaceholders.ENTITY_UUID, entity)
                .put(TextPlaceholders.ENTITY_TYPE, entity)
                .build();
    }

    @NotNull
    public static TextContext world(@NotNull World world) {
        return TextContext.builder().put(TextPlaceholders.WORLD, Objects.requireNonNull(world, "world")).build();
    }

    @NotNull
    public static TextContext location(@NotNull Location location) {
        Objects.requireNonNull(location, "location");
        TextContext.Builder builder = TextContext.builder()
                .put(TextPlaceholders.X, location)
                .put(TextPlaceholders.Y, location)
                .put(TextPlaceholders.Z, location);
        if (location.getWorld() != null) {
            builder.put(TextPlaceholders.WORLD, location.getWorld());
        }
        return builder.build();
    }

    private TextContexts() {
    }
}
