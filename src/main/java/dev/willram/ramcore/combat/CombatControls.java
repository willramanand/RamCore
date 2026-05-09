package dev.willram.ramcore.combat;

import dev.willram.ramcore.entity.EntityAttributeSpec;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Static factories for attribute and combat helpers.
 */
public final class CombatControls {

    @NotNull
    public static EntityAttributeSpec.Builder attributes() {
        return EntityAttributeSpec.builder();
    }

    @NotNull
    public static AttributeModifierSpec.Builder modifier(@NotNull NamespacedKey key) {
        return AttributeModifierSpec.builder(key);
    }

    @NotNull
    public static CombatProfile.Builder profile() {
        return CombatProfile.builder();
    }

    @NotNull
    public static DamageProfile.Builder damage(double amount) {
        return DamageProfile.amount(amount);
    }

    @NotNull
    public static AttributeBuff buff(@NotNull LivingEntity entity, @NotNull EntityAttributeSpec spec) {
        return AttributeBuff.apply(entity, spec);
    }

    @NotNull
    public static AttributeBuff buff(@NotNull LivingEntity entity, @NotNull CombatProfile profile) {
        return profile.temporary(entity);
    }

    @NotNull
    public static EntityAttributeSpec damageProfile(double movementSpeed, double followRange, double armor,
                                                    double armorToughness, double scale) {
        return attributes()
                .base(Attribute.MOVEMENT_SPEED, movementSpeed)
                .base(Attribute.FOLLOW_RANGE, followRange)
                .base(Attribute.ARMOR, armor)
                .base(Attribute.ARMOR_TOUGHNESS, armorToughness)
                .base(Attribute.SCALE, scale)
                .build();
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.ENTITY_ATTRIBUTES,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper-entity-attributes",
                null,
                null,
                "Paper exposes vanilla attributes, modifiers, invulnerability ticks, and damage application; attack cooldown internals, hurt timers, and custom routing need a versioned adapter."
        ))
                .override(NmsCapabilityCheck.supported(
                        NmsCapability.ATTRIBUTE_MODIFIERS,
                        NmsAccessTier.PAPER_API,
                        "paper-entity-attributes",
                        "Paper exposes vanilla attribute instances and modifiers."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.DAMAGE_APPLICATION,
                        NmsAccessTier.PAPER_API,
                        "paper-damage",
                        "Bukkit/Paper expose damage calls and DamageSource where available; custom routing remains version-specific."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.COMBAT_INVULNERABILITY_TICKS,
                        NmsAccessTier.PAPER_API,
                        "paper-damage",
                        "Bukkit/Paper expose no-damage tick controls; deeper hurt timer internals remain adapter-only."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.COMBAT_ATTACK_COOLDOWN,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Attack cooldown internals require a versioned adapter."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.COMBAT_HURT_TIMERS,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Hurt timers and attack animations require a versioned adapter."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.COMBAT_DAMAGE_ROUTING,
                        NmsAccessTier.VERSIONED_IMPLEMENTATION,
                        "none",
                        "Custom damage routing requires a versioned adapter."
                ));
        return registry;
    }

    private CombatControls() {
    }
}
