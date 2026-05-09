package dev.willram.ramcore.entity;

import dev.willram.ramcore.path.PathController;
import dev.willram.ramcore.path.PathRequest;
import dev.willram.ramcore.path.PathTask;
import dev.willram.ramcore.path.Pathfinders;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Fluent Paper-first wrapper for common entity control operations.
 */
public final class EntityControl<T extends Entity> {
    private final T entity;

    EntityControl(@NotNull T entity) {
        this.entity = Objects.requireNonNull(entity, "entity");
    }

    @NotNull
    public T entity() {
        return this.entity;
    }

    @NotNull
    public EntityControl<T> lookAt(@NotNull Location location) {
        return lookAt(location, LookAnchor.EYES);
    }

    @NotNull
    public EntityControl<T> lookAt(@NotNull Location location, @NotNull LookAnchor anchor) {
        Objects.requireNonNull(location, "location");
        this.entity.lookAt(location.getX(), location.getY(), location.getZ(), Objects.requireNonNull(anchor, "anchor"));
        return this;
    }

    @NotNull
    public EntityControl<T> lookAt(@NotNull Entity target) {
        return lookAt(Objects.requireNonNull(target, "target").getLocation());
    }

    @NotNull
    public EntityControl<T> velocity(@NotNull Vector velocity) {
        this.entity.setVelocity(Objects.requireNonNull(velocity, "velocity").clone());
        return this;
    }

    public boolean teleport(@NotNull Location location) {
        return this.entity.teleport(Objects.requireNonNull(location, "location"));
    }

    public boolean teleport(@NotNull Location location, @NotNull PlayerTeleportEvent.TeleportCause cause) {
        return this.entity.teleport(Objects.requireNonNull(location, "location"), Objects.requireNonNull(cause, "cause"));
    }

    @NotNull
    public EntityControl<T> invulnerable(boolean invulnerable) {
        this.entity.setInvulnerable(invulnerable);
        return this;
    }

    @NotNull
    public EntityControl<T> persistent(boolean persistent) {
        this.entity.setPersistent(persistent);
        return this;
    }

    @NotNull
    public EntityControl<T> gravity(boolean gravity) {
        this.entity.setGravity(gravity);
        return this;
    }

    @NotNull
    public EntityControl<T> silent(boolean silent) {
        this.entity.setSilent(silent);
        return this;
    }

    @NotNull
    public EntityControl<T> glowing(boolean glowing) {
        this.entity.setGlowing(glowing);
        return this;
    }

    @NotNull
    public EntityControl<T> invisible(boolean invisible) {
        this.entity.setInvisible(invisible);
        return this;
    }

    @NotNull
    public EntityControl<T> scoreboardTag(@NotNull String tag) {
        this.entity.addScoreboardTag(Objects.requireNonNull(tag, "tag"));
        return this;
    }

    @NotNull
    public EntityControl<T> removeScoreboardTag(@NotNull String tag) {
        this.entity.removeScoreboardTag(Objects.requireNonNull(tag, "tag"));
        return this;
    }

    @NotNull
    public EntityControl<T> ai(boolean ai) {
        living().setAI(ai);
        return this;
    }

    @NotNull
    public EntityControl<T> collidable(boolean collidable) {
        living().setCollidable(collidable);
        return this;
    }

    @NotNull
    public EntityControl<T> removeWhenFarAway(boolean removeWhenFarAway) {
        living().setRemoveWhenFarAway(removeWhenFarAway);
        return this;
    }

    @NotNull
    public EntityControl<T> canPickupItems(boolean canPickupItems) {
        living().setCanPickupItems(canPickupItems);
        return this;
    }

    @NotNull
    public EntityControl<T> jumping(boolean jumping) {
        living().setJumping(jumping);
        return this;
    }

    @NotNull
    public EntityControl<T> target(LivingEntity target) {
        mob().setTarget(target);
        return this;
    }

    @NotNull
    public EntityControl<T> clearTarget() {
        mob().setTarget(null);
        return this;
    }

    @NotNull
    public EntityControl<T> aware(boolean aware) {
        mob().setAware(aware);
        return this;
    }

    @NotNull
    public PathController pathController() {
        return Pathfinders.controller(mob());
    }

    @NotNull
    public PathTask path(@NotNull PathRequest request) {
        return pathController().schedule(request);
    }

    public boolean angry(boolean angry) {
        return invokeBooleanSetter("setAngry", angry);
    }

    public boolean anger(int ticks) {
        return invokeIntSetter("setAnger", ticks);
    }

    @NotNull
    public EntityTemporaryModifier<T> temporary() {
        return new EntityTemporaryModifier<>(this.entity);
    }

    @NotNull
    private LivingEntity living() {
        if (this.entity instanceof LivingEntity living) {
            return living;
        }
        throw new IllegalStateException("entity is not a LivingEntity: " + this.entity.getType());
    }

    @NotNull
    private Mob mob() {
        if (this.entity instanceof Mob mob) {
            return mob;
        }
        throw new IllegalStateException("entity is not a Mob: " + this.entity.getType());
    }

    private boolean invokeBooleanSetter(String name, boolean value) {
        try {
            Method method = this.entity.getClass().getMethod(name, boolean.class);
            method.invoke(this.entity, value);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean invokeIntSetter(String name, int value) {
        try {
            Method method = this.entity.getClass().getMethod(name, int.class);
            method.invoke(this.entity, value);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
