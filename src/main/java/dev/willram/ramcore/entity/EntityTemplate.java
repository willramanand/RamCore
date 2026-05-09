package dev.willram.ramcore.entity;

import dev.willram.ramcore.ai.MobAi;
import dev.willram.ramcore.ai.MobAiController;
import dev.willram.ramcore.metadata.Metadata;
import dev.willram.ramcore.metadata.MetadataKey;
import dev.willram.ramcore.path.PathNavigationProfile;
import dev.willram.ramcore.path.Pathfinders;
import dev.willram.ramcore.pdc.PdcEditor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Configurable server-backed living entity template.
 */
public final class EntityTemplate<T extends LivingEntity> {
    private final Class<T> type;
    private Component name;
    private Boolean nameVisible;
    private Boolean visibleByDefault;
    private Boolean invulnerable;
    private Boolean gravity;
    private Boolean silent;
    private Boolean glowing;
    private Boolean persistent;
    private Boolean invisible;
    private Boolean ai;
    private Boolean collidable;
    private Boolean removeWhenFarAway;
    private Boolean canPickupItems;
    private Boolean aware;
    private CreatureSpawnEvent.SpawnReason spawnReason = CreatureSpawnEvent.SpawnReason.CUSTOM;
    private boolean randomizeData;
    private EntityEquipmentSpec equipmentSpec;
    private EntityAttributeSpec attributeSpec;
    private PathNavigationProfile pathNavigationProfile = PathNavigationProfile.unchanged();
    private final Set<String> scoreboardTags = new LinkedHashSet<>();
    private final Map<MetadataKey<?>, Object> metadata = new LinkedHashMap<>();
    private final List<EntityTemplate<? extends LivingEntity>> passengers = new ArrayList<>();
    private Consumer<PdcEditor> pdcCustomizer = ignored -> {};
    private boolean hasPdcCustomizer;
    private Consumer<MobAiController<Mob>> aiCustomizer = ignored -> {};
    private boolean hasAiCustomizer;
    private Consumer<T> customizer = ignored -> {};

    private EntityTemplate(@NotNull Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    @NotNull
    public static <T extends LivingEntity> EntityTemplate<T> of(@NotNull Class<T> type) {
        return new EntityTemplate<>(type);
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @NotNull
    public CreatureSpawnEvent.SpawnReason spawnReason() {
        return this.spawnReason;
    }

    public boolean randomizeData() {
        return this.randomizeData;
    }

    @NotNull
    public List<EntityTemplate<? extends LivingEntity>> passengers() {
        return List.copyOf(this.passengers);
    }

    @NotNull
    public EntityTemplate<T> name(@NotNull ComponentLike name) {
        this.name = Objects.requireNonNull(name, "name").asComponent();
        return this;
    }

    @NotNull
    public EntityTemplate<T> nameVisible(boolean nameVisible) {
        this.nameVisible = nameVisible;
        return this;
    }

    @NotNull
    public EntityTemplate<T> visibleByDefault(boolean visibleByDefault) {
        this.visibleByDefault = visibleByDefault;
        return this;
    }

    @NotNull
    public EntityTemplate<T> invulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        return this;
    }

    @NotNull
    public EntityTemplate<T> gravity(boolean gravity) {
        this.gravity = gravity;
        return this;
    }

    @NotNull
    public EntityTemplate<T> silent(boolean silent) {
        this.silent = silent;
        return this;
    }

    @NotNull
    public EntityTemplate<T> glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    @NotNull
    public EntityTemplate<T> persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    @NotNull
    public EntityTemplate<T> invisible(boolean invisible) {
        this.invisible = invisible;
        return this;
    }

    @NotNull
    public EntityTemplate<T> ai(boolean ai) {
        this.ai = ai;
        return this;
    }

    @NotNull
    public EntityTemplate<T> collidable(boolean collidable) {
        this.collidable = collidable;
        return this;
    }

    @NotNull
    public EntityTemplate<T> removeWhenFarAway(boolean removeWhenFarAway) {
        this.removeWhenFarAway = removeWhenFarAway;
        return this;
    }

    @NotNull
    public EntityTemplate<T> canPickupItems(boolean canPickupItems) {
        this.canPickupItems = canPickupItems;
        return this;
    }

    @NotNull
    public EntityTemplate<T> aware(boolean aware) {
        this.aware = aware;
        return this;
    }

    @NotNull
    public EntityTemplate<T> spawnReason(@NotNull CreatureSpawnEvent.SpawnReason spawnReason) {
        this.spawnReason = Objects.requireNonNull(spawnReason, "spawnReason");
        return this;
    }

    @NotNull
    public EntityTemplate<T> randomizeData(boolean randomizeData) {
        this.randomizeData = randomizeData;
        return this;
    }

    @NotNull
    public EntityTemplate<T> equipment(@NotNull EntityEquipmentSpec equipmentSpec) {
        this.equipmentSpec = Objects.requireNonNull(equipmentSpec, "equipmentSpec");
        return this;
    }

    @NotNull
    public EntityTemplate<T> attributes(@NotNull EntityAttributeSpec attributeSpec) {
        this.attributeSpec = Objects.requireNonNull(attributeSpec, "attributeSpec");
        return this;
    }

    @NotNull
    public EntityTemplate<T> pathNavigation(@NotNull PathNavigationProfile profile) {
        this.pathNavigationProfile = Objects.requireNonNull(profile, "profile");
        return this;
    }

    @NotNull
    public EntityTemplate<T> scoreboardTag(@NotNull String tag) {
        this.scoreboardTags.add(Objects.requireNonNull(tag, "tag"));
        return this;
    }

    @NotNull
    public <V> EntityTemplate<T> metadata(@NotNull MetadataKey<V> key, @NotNull V value) {
        this.metadata.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return this;
    }

    @NotNull
    public EntityTemplate<T> pdc(@NotNull Consumer<PdcEditor> customizer) {
        this.pdcCustomizer = this.pdcCustomizer.andThen(Objects.requireNonNull(customizer, "customizer"));
        this.hasPdcCustomizer = true;
        return this;
    }

    @NotNull
    public EntityTemplate<T> mobAi(@NotNull Consumer<MobAiController<Mob>> customizer) {
        this.aiCustomizer = this.aiCustomizer.andThen(Objects.requireNonNull(customizer, "customizer"));
        this.hasAiCustomizer = true;
        return this;
    }

    @NotNull
    public EntityTemplate<T> passenger(@NotNull EntityTemplate<? extends LivingEntity> passenger) {
        this.passengers.add(Objects.requireNonNull(passenger, "passenger"));
        return this;
    }

    @NotNull
    public EntityTemplate<T> customize(@NotNull Consumer<T> customizer) {
        this.customizer = this.customizer.andThen(Objects.requireNonNull(customizer, "customizer"));
        return this;
    }

    public void apply(@NotNull T entity) {
        Objects.requireNonNull(entity, "entity");
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
        if (this.invisible != null) {
            entity.setInvisible(this.invisible);
        }
        applyLiving(entity);
        if (this.hasPdcCustomizer) {
            this.pdcCustomizer.accept(PdcEditor.of(entity.getPersistentDataContainer()));
        }
        this.metadata.forEach((key, value) -> putMetadata(entity, key, value));
        this.customizer.accept(entity);
    }

    private void applyLiving(T entity) {
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
        if (this.equipmentSpec != null) {
            this.equipmentSpec.apply(entity);
        }
        if (this.attributeSpec != null) {
            this.attributeSpec.apply(entity);
        }
        for (String tag : this.scoreboardTags) {
            entity.addScoreboardTag(tag);
        }
        if (entity instanceof Mob mob) {
            applyMob(mob);
        }
    }

    private void applyMob(Mob mob) {
        if (this.aware != null) {
            mob.setAware(this.aware);
        }
        if (this.pathNavigationProfile.hasChanges()) {
            Pathfinders.paperBackend(mob).applyNavigationProfile(this.pathNavigationProfile);
        }
        if (this.hasAiCustomizer) {
            this.aiCustomizer.accept(MobAi.controller(mob));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void putMetadata(Entity entity, MetadataKey key, Object value) {
        Metadata.provideForEntity(entity).put(key, value);
    }
}
