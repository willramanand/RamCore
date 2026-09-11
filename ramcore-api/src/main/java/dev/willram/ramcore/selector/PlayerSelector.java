package dev.willram.ramcore.selector;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Player-focused selector with common player predicates.
 */
public final class PlayerSelector {
    private final EntitySelector<Player> delegate;

    PlayerSelector() {
        this.delegate = new EntitySelector<>(Player.class);
    }

    @NotNull
    public PlayerSelector filter(@NotNull Predicate<? super Player> filter) {
        this.delegate.filter(filter);
        return this;
    }

    @NotNull
    public PlayerSelector uuid(@NotNull UUID uuid) {
        this.delegate.uuid(uuid);
        return this;
    }

    @NotNull
    public PlayerSelector name(@NotNull String name) {
        this.delegate.name(name);
        return this;
    }

    @NotNull
    public PlayerSelector world(@NotNull World world) {
        this.delegate.world(world);
        return this;
    }

    @NotNull
    public PlayerSelector within(@NotNull Location origin, double radius) {
        this.delegate.within(origin, radius);
        return this;
    }

    @NotNull
    public PlayerSelector nearest(@NotNull Location origin) {
        this.delegate.nearest(origin);
        return this;
    }

    @NotNull
    public PlayerSelector farthest(@NotNull Location origin) {
        this.delegate.farthest(origin);
        return this;
    }

    @NotNull
    public PlayerSelector random() {
        this.delegate.random();
        return this;
    }

    @NotNull
    public PlayerSelector sort(@NotNull SelectorSort sort) {
        this.delegate.sort(sort);
        return this;
    }

    @NotNull
    public PlayerSelector limit(int limit) {
        this.delegate.limit(limit);
        return this;
    }

    @NotNull
    public PlayerSelector permission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return filter(player -> player.hasPermission(permission));
    }

    @NotNull
    public PlayerSelector gameMode(@NotNull GameMode gameMode) {
        Objects.requireNonNull(gameMode, "gameMode");
        return filter(player -> player.getGameMode() == gameMode);
    }

    @NotNull
    public PlayerSelector online() {
        return filter(Player::isOnline);
    }

    @NotNull
    public List<Player> select(@NotNull Collection<? extends Player> players) {
        return this.delegate.select(players);
    }

    @NotNull
    public Optional<Player> first(@NotNull Collection<? extends Player> players) {
        return this.delegate.first(players);
    }

    @NotNull
    public Player single(@NotNull Collection<? extends Player> players) {
        return this.delegate.single(players);
    }

    @NotNull
    public EntitySelector<Player> entitySelector() {
        return this.delegate;
    }
}
