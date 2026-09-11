package dev.willram.ramcore.combat;

import dev.willram.ramcore.entity.EntityAttributeSpec;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Attribute-backed combat profile for custom mobs.
 */
public final class CombatProfile {
    private final EntityAttributeSpec attributes;

    private CombatProfile(EntityAttributeSpec attributes) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public EntityAttributeSpec attributes() {
        return this.attributes;
    }

    public void apply(@NotNull LivingEntity entity) {
        this.attributes.apply(entity);
    }

    @NotNull
    public AttributeBuff temporary(@NotNull LivingEntity entity) {
        return AttributeBuff.apply(entity, this.attributes);
    }

    public static final class Builder {
        private final EntityAttributeSpec.Builder attributes = EntityAttributeSpec.builder();

        public Builder base(@NotNull Attribute attribute, double value) {
            this.attributes.base(attribute, value);
            return this;
        }

        public Builder modifier(@NotNull Attribute attribute, @NotNull AttributeModifierSpec modifier) {
            this.attributes.modifier(attribute, modifier);
            return this;
        }

        public Builder knockbackResistance(double value) {
            return base(Attribute.KNOCKBACK_RESISTANCE, value);
        }

        public Builder attackReach(double value) {
            return base(Attribute.ENTITY_INTERACTION_RANGE, value);
        }

        public Builder movementSpeed(double value) {
            return base(Attribute.MOVEMENT_SPEED, value);
        }

        public Builder followRange(double value) {
            return base(Attribute.FOLLOW_RANGE, value);
        }

        public Builder armor(double value) {
            return base(Attribute.ARMOR, value);
        }

        public Builder armorToughness(double value) {
            return base(Attribute.ARMOR_TOUGHNESS, value);
        }

        public Builder scale(double value) {
            return base(Attribute.SCALE, value);
        }

        public Builder safeFallDistance(double value) {
            return base(Attribute.SAFE_FALL_DISTANCE, value);
        }

        public Builder gravity(double value) {
            return base(Attribute.GRAVITY, value);
        }

        public Builder stepHeight(double value) {
            return base(Attribute.STEP_HEIGHT, value);
        }

        public CombatProfile build() {
            return new CombatProfile(this.attributes.build());
        }
    }
}
