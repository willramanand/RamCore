package dev.willram.ramcore.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Configuration for a server-backed NPC entity.
 */
public final class NpcSpec<T extends Entity> {
    private final Class<T> type;
    private Component name;
    private Boolean nameVisible;
    private Boolean visibleByDefault;
    private Boolean invulnerable;
    private Boolean gravity;
    private Boolean silent;
    private Boolean glowing;
    private Boolean persistent;
    private Boolean ai;
    private Boolean collidable;
    private Boolean removeWhenFarAway;
    private Boolean canPickupItems;
    private boolean consumeClicks = true;
    private NpcClickHandler clickHandler = ignored -> {
    };
    private Consumer<T> customizer = ignored -> {
    };

    private NpcSpec(@NotNull Class<T> type) {
        this.type = requireNonNull(type, "type");
    }

    @NotNull
    public static <T extends Entity> NpcSpec<T> of(@NotNull Class<T> type) {
        return new NpcSpec<>(type);
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @NotNull
    public NpcSpec<T> name(@NotNull ComponentLike name) {
        this.name = requireNonNull(name, "name").asComponent();
        return this;
    }

    @NotNull
    public NpcSpec<T> nameVisible(boolean nameVisible) {
        this.nameVisible = nameVisible;
        return this;
    }

    @NotNull
    public NpcSpec<T> visibleByDefault(boolean visibleByDefault) {
        this.visibleByDefault = visibleByDefault;
        return this;
    }

    @NotNull
    public NpcSpec<T> invulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        return this;
    }

    @NotNull
    public NpcSpec<T> gravity(boolean gravity) {
        this.gravity = gravity;
        return this;
    }

    @NotNull
    public NpcSpec<T> silent(boolean silent) {
        this.silent = silent;
        return this;
    }

    @NotNull
    public NpcSpec<T> glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    @NotNull
    public NpcSpec<T> persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    @NotNull
    public NpcSpec<T> ai(boolean ai) {
        this.ai = ai;
        return this;
    }

    @NotNull
    public NpcSpec<T> collidable(boolean collidable) {
        this.collidable = collidable;
        return this;
    }

    @NotNull
    public NpcSpec<T> removeWhenFarAway(boolean removeWhenFarAway) {
        this.removeWhenFarAway = removeWhenFarAway;
        return this;
    }

    @NotNull
    public NpcSpec<T> canPickupItems(boolean canPickupItems) {
        this.canPickupItems = canPickupItems;
        return this;
    }

    @NotNull
    public NpcSpec<T> consumeClicks(boolean consumeClicks) {
        this.consumeClicks = consumeClicks;
        return this;
    }

    public boolean consumeClicks() {
        return this.consumeClicks;
    }

    @NotNull
    public NpcSpec<T> onClick(@NotNull NpcClickHandler handler) {
        this.clickHandler = requireNonNull(handler, "handler");
        return this;
    }

    @NotNull
    public NpcClickHandler clickHandler() {
        return this.clickHandler;
    }

    @NotNull
    public NpcSpec<T> customize(@NotNull Consumer<T> customizer) {
        this.customizer = requireNonNull(customizer, "customizer");
        return this;
    }

    public void apply(@NotNull T entity) {
        requireNonNull(entity, "entity");
        if (this.name != null) {
            entity.customName(this.name);
        }
        if (this.nameVisible != null) {
            entity.setCustomNameVisible(this.nameVisible);
        }
        if (this.visibleByDefault != null) {
            entity.setVisibleByDefault(this.visibleByDefault);
        }
        if (this.invulnerable != null) {
            entity.setInvulnerable(this.invulnerable);
        }
        if (this.gravity != null) {
            entity.setGravity(this.gravity);
        }
        if (this.silent != null) {
            entity.setSilent(this.silent);
        }
        if (this.glowing != null) {
            entity.setGlowing(this.glowing);
        }
        if (this.persistent != null) {
            entity.setPersistent(this.persistent);
        }
        if (entity instanceof LivingEntity living) {
            applyLiving(living);
        }
        this.customizer.accept(entity);
    }

    private void applyLiving(@NotNull LivingEntity entity) {
        if (this.ai != null) {
            entity.setAI(this.ai);
        }
        if (this.collidable != null) {
            entity.setCollidable(this.collidable);
        }
        if (this.removeWhenFarAway != null) {
            entity.setRemoveWhenFarAway(this.removeWhenFarAway);
        }
        if (this.canPickupItems != null) {
            entity.setCanPickupItems(this.canPickupItems);
        }
    }
}
