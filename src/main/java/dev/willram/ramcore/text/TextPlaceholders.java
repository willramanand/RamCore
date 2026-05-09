package dev.willram.ramcore.text;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Common typed placeholder declarations.
 */
public final class TextPlaceholders {
    public static final TextPlaceholder<Player> PLAYER =
            TextPlaceholder.unparsed("player", Player.class, Player::getName);
    public static final TextPlaceholder<Player> PLAYER_UUID =
            TextPlaceholder.unparsed("player_uuid", Player.class, player -> player.getUniqueId().toString());
    public static final TextPlaceholder<Entity> ENTITY =
            TextPlaceholder.unparsed("entity", Entity.class, Entity::getName);
    public static final TextPlaceholder<Entity> ENTITY_UUID =
            TextPlaceholder.unparsed("entity_uuid", Entity.class, entity -> entity.getUniqueId().toString());
    public static final TextPlaceholder<Entity> ENTITY_TYPE =
            TextPlaceholder.unparsed("entity_type", Entity.class, entity -> entity.getType().name().toLowerCase());
    public static final TextPlaceholder<World> WORLD =
            TextPlaceholder.unparsed("world", World.class, World::getName);
    public static final TextPlaceholder<Location> X =
            TextPlaceholder.unparsed("x", Location.class, location -> Texts.plainNumber(location.getX()));
    public static final TextPlaceholder<Location> Y =
            TextPlaceholder.unparsed("y", Location.class, location -> Texts.plainNumber(location.getY()));
    public static final TextPlaceholder<Location> Z =
            TextPlaceholder.unparsed("z", Location.class, location -> Texts.plainNumber(location.getZ()));

    private TextPlaceholders() {
    }
}
