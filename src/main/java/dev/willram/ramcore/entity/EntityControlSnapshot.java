package dev.willram.ramcore.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Restorable snapshot of common Paper-exposed entity controls.
 */
public final class EntityControlSnapshot {
    private final Component customName;
    private final boolean customNameVisible;
    private final boolean visibleByDefault;
    private final boolean invulnerable;
    private final boolean gravity;
    private final boolean silent;
    private final boolean glowing;
    private final boolean persistent;
    private final boolean invisible;
    private final Vector velocity;
    private final Set<String> scoreboardTags;
    private final LivingState livingState;
    private final MobState mobState;

    private EntityControlSnapshot(@NotNull Entity entity) {
        this.customName = entity.customName();
        this.customNameVisible = entity.isCustomNameVisible();
        this.visibleByDefault = entity.isVisibleByDefault();
        this.invulnerable = entity.isInvulnerable();
        this.gravity = entity.hasGravity();
        this.silent = entity.isSilent();
        this.glowing = entity.isGlowing();
        this.persistent = entity.isPersistent();
        this.invisible = entity.isInvisible();
        this.velocity = entity.getVelocity().clone();
        this.scoreboardTags = new LinkedHashSet<>(entity.getScoreboardTags());
        this.livingState = entity instanceof LivingEntity living ? new LivingState(living) : null;
        this.mobState = entity instanceof Mob mob ? new MobState(mob) : null;
    }

    @NotNull
    public static EntityControlSnapshot capture(@NotNull Entity entity) {
        return new EntityControlSnapshot(Objects.requireNonNull(entity, "entity"));
    }

    public void restore(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "entity");
        entity.customName(this.customName);
        entity.setCustomNameVisible(this.customNameVisible);
        entity.setVisibleByDefault(this.visibleByDefault);
        entity.setInvulnerable(this.invulnerable);
        entity.setGravity(this.gravity);
        entity.setSilent(this.silent);
        entity.setGlowing(this.glowing);
        entity.setPersistent(this.persistent);
        entity.setInvisible(this.invisible);
        entity.setVelocity(this.velocity.clone());
        restoreScoreboardTags(entity);
        if (entity instanceof LivingEntity living && this.livingState != null) {
            this.livingState.restore(living);
        }
        if (entity instanceof Mob mob && this.mobState != null) {
            this.mobState.restore(mob);
        }
    }

    private void restoreScoreboardTags(Entity entity) {
        for (String tag : Set.copyOf(entity.getScoreboardTags())) {
            if (!this.scoreboardTags.contains(tag)) {
                entity.removeScoreboardTag(tag);
            }
        }
        for (String tag : this.scoreboardTags) {
            entity.addScoreboardTag(tag);
        }
    }

    private static final class LivingState {
        private final boolean ai;
        private final boolean collidable;
        private final boolean removeWhenFarAway;
        private final boolean canPickupItems;

        private LivingState(LivingEntity entity) {
            this.ai = entity.hasAI();
            this.collidable = entity.isCollidable();
            this.removeWhenFarAway = entity.getRemoveWhenFarAway();
            this.canPickupItems = entity.getCanPickupItems();
        }

        private void restore(LivingEntity entity) {
            entity.setAI(this.ai);
            entity.setCollidable(this.collidable);
            entity.setRemoveWhenFarAway(this.removeWhenFarAway);
            entity.setCanPickupItems(this.canPickupItems);
        }
    }

    private static final class MobState {
        private final boolean aware;
        private final LivingEntity target;

        private MobState(Mob mob) {
            this.aware = mob.isAware();
            this.target = mob.getTarget();
        }

        private void restore(Mob mob) {
            mob.setAware(this.aware);
            mob.setTarget(this.target);
        }
    }
}
