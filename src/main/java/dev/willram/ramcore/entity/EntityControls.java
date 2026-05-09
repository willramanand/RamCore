package dev.willram.ramcore.entity;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.combat.AttributeBuff;
import dev.willram.ramcore.combat.CombatControls;
import dev.willram.ramcore.combat.CombatProfile;
import dev.willram.ramcore.combat.DamageProfile;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Static factory methods for entity control utilities.
 */
public final class EntityControls {

    @NotNull
    public static <T extends Entity> EntityControl<T> control(@NotNull T entity) {
        return new EntityControl<>(entity);
    }

    @NotNull
    public static <T extends Entity> EntityTemporaryModifier<T> temporary(@NotNull T entity) {
        return control(entity).temporary();
    }

    @NotNull
    public static EntityControlSnapshot snapshot(@NotNull Entity entity) {
        return EntityControlSnapshot.capture(entity);
    }

    @NotNull
    public static <T extends LivingEntity> EntityTemplate<T> template(@NotNull Class<T> type) {
        return EntityTemplate.of(type);
    }

    @NotNull
    public static EntityEquipmentSpec.Builder equipment() {
        return EntityEquipmentSpec.builder();
    }

    @NotNull
    public static EntityAttributeSpec.Builder attributes() {
        return CombatControls.attributes();
    }

    @NotNull
    public static CombatProfile.Builder combatProfile() {
        return CombatControls.profile();
    }

    @NotNull
    public static DamageProfile.Builder damage(double amount) {
        return CombatControls.damage(amount);
    }

    public static void damage(@NotNull Damageable target, @NotNull DamageProfile profile) {
        profile.apply(target);
    }

    @NotNull
    public static AttributeBuff buff(@NotNull LivingEntity entity, @NotNull EntityAttributeSpec spec) {
        return CombatControls.buff(entity, spec);
    }

    @NotNull
    public static <T extends LivingEntity> EntitySpawnHandle<T> spawnNow(@NotNull Location location,
                                                                         @NotNull EntityTemplate<T> template) {
        return ConfiguredEntitySpawner.spawnNow(
                Objects.requireNonNull(location, "location").getWorld(),
                location,
                template
        );
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.ENTITY_CONTROL,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper-entity-control",
                null,
                null,
                "Paper exposes common entity flags, target selection, pickup rules, equipment drops, attributes, and spawning; raw look/move/jump controllers need a versioned adapter."
        ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.ENTITY_SPAWNING,
                        NmsAccessTier.PAPER_API,
                        "paper-entity-control",
                        "Paper exposes configured entity spawn callbacks and spawn reasons."
                ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.ENTITY_TEMPLATES,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-entity-templates",
                        "RamCore applies entity templates using public Bukkit/Paper state surfaces."
                ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.ENTITY_TEMPORARY_MODIFIERS,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-entity-snapshots",
                        "RamCore captures and restores supported entity state through terminable modifiers."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.ENTITY_EQUIPMENT,
                        NmsAccessTier.PAPER_API,
                        "paper-entity-control",
                        "Paper exposes equipment and drop chances for living entities; deeper item internals are handled by item capabilities."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.ENTITY_LOOK_CONTROL,
                        NmsAccessTier.PAPER_API,
                        "paper-entity-control",
                        "Bukkit/Paper expose orientation through location and teleport APIs; raw look controllers require an adapter."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.ENTITY_MOVE_CONTROL,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Raw entity move-control internals require a versioned adapter."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.ENTITY_JUMP_CONTROL,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Raw entity jump-control internals require a versioned adapter."
                ));
        return registry;
    }

    private EntityControls() {
    }
}
