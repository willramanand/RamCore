package dev.willram.ramcore.selector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade for reusable player and entity selectors.
 */
public final class Selectors {

    @NotNull
    public static EntitySelector<Entity> entities() {
        return entity(Entity.class);
    }

    @NotNull
    public static <T extends Entity> EntitySelector<T> entity(@NotNull Class<T> type) {
        return new EntitySelector<>(type);
    }

    @NotNull
    public static PlayerSelector players() {
        return new PlayerSelector();
    }

    @NotNull
    public static List<Player> onlinePlayers(@NotNull PlayerSelector selector) {
        Objects.requireNonNull(selector, "selector");
        return selector.select(Bukkit.getOnlinePlayers());
    }

    @NotNull
    public static Optional<Player> playerByName(@NotNull Collection<? extends Player> players, @NotNull String name) {
        return players().name(name).first(players);
    }

    @NotNull
    public static Optional<Player> playerByUuid(@NotNull Collection<? extends Player> players, @NotNull UUID uuid) {
        return players().uuid(uuid).first(players);
    }

    @NotNull
    public static Optional<Entity> entityByUuid(@NotNull Collection<? extends Entity> entities, @NotNull UUID uuid) {
        return entities().uuid(uuid).first(entities);
    }

    private Selectors() {
    }
}
