package dev.willram.ramcore.npc;

import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.Terminable;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Owned spawned NPC entity.
 */
public final class NpcHandle<T extends Entity> implements Terminable {
    private final T entity;
    private final NpcSpec<T> spec;

    NpcHandle(@NotNull T entity, @NotNull NpcSpec<T> spec) {
        this.entity = requireNonNull(entity, "entity");
        this.spec = requireNonNull(spec, "spec");
    }

    @NotNull
    public T entity() {
        return this.entity;
    }

    @NotNull
    public NpcSpec<T> spec() {
        return this.spec;
    }

    public boolean valid() {
        return this.entity.isValid() && !this.entity.isDead();
    }

    public void teleport(@NotNull Location location) {
        requireNonNull(location, "location");
        Schedulers.run(this.entity, () -> this.entity.teleport(location));
    }

    public void lookAt(@NotNull Location location) {
        requireNonNull(location, "location");
        Schedulers.run(this.entity, () -> this.entity.lookAt(location.getX(), location.getY(), location.getZ(), LookAnchor.EYES));
    }

    public void lookAt(@NotNull Player player) {
        requireNonNull(player, "player");
        Location eye = player.getEyeLocation();
        lookAt(eye);
    }

    public void hideFrom(@NotNull Plugin plugin, @NotNull Player player) {
        requireNonNull(player, "player").hideEntity(requireNonNull(plugin, "plugin"), this.entity);
    }

    public void showTo(@NotNull Plugin plugin, @NotNull Player player) {
        requireNonNull(player, "player").showEntity(requireNonNull(plugin, "plugin"), this.entity);
    }

    public boolean visibleTo(@NotNull Player player) {
        return requireNonNull(player, "player").canSee(this.entity);
    }

    public void click(@NotNull Player player, @NotNull NpcClickType type) {
        requireNonNull(player, "player");
        requireNonNull(type, "type");
        this.spec.clickHandler().click(new NpcClickContext(this, player, this.entity, type));
    }

    @Override
    public void close() {
        Schedulers.run(this.entity, this.entity::remove);
    }

    @Override
    public boolean isClosed() {
        return !valid();
    }
}
