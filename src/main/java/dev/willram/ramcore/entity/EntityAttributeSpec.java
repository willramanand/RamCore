package dev.willram.ramcore.entity;

import dev.willram.ramcore.combat.AttributeModifierSpec;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base attribute values to apply to a living entity.
 */
public final class EntityAttributeSpec {
    private final Map<Attribute, Double> baseValues;
    private final Map<Attribute, List<AttributeModifierSpec>> modifiers;

    private EntityAttributeSpec(Map<Attribute, Double> baseValues, Map<Attribute, List<AttributeModifierSpec>> modifiers) {
        this.baseValues = Map.copyOf(baseValues);
        this.modifiers = copyModifiers(modifiers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void apply(@NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        for (Map.Entry<Attribute, Double> entry : this.baseValues.entrySet()) {
            AttributeInstance attribute = entity.getAttribute(entry.getKey());
            if (attribute != null) {
                attribute.setBaseValue(entry.getValue());
            }
        }
        for (Map.Entry<Attribute, List<AttributeModifierSpec>> entry : this.modifiers.entrySet()) {
            AttributeInstance attribute = entity.getAttribute(entry.getKey());
            if (attribute == null) {
                continue;
            }
            for (AttributeModifierSpec modifier : entry.getValue()) {
                attribute.removeModifier(modifier.key());
                if (modifier.transientModifier()) {
                    attribute.addTransientModifier(modifier.toBukkit());
                } else {
                    attribute.addModifier(modifier.toBukkit());
                }
            }
        }
    }

    @NotNull
    public Map<Attribute, Double> baseValues() {
        return this.baseValues;
    }

    @NotNull
    public Map<Attribute, List<AttributeModifierSpec>> modifiers() {
        return this.modifiers;
    }

    private static Map<Attribute, List<AttributeModifierSpec>> copyModifiers(Map<Attribute, List<AttributeModifierSpec>> source) {
        Map<Attribute, List<AttributeModifierSpec>> copy = new LinkedHashMap<>();
        source.forEach((attribute, modifiers) -> copy.put(attribute, List.copyOf(modifiers)));
        return Collections.unmodifiableMap(copy);
    }

    public static final class Builder {
        private final Map<Attribute, Double> baseValues = new LinkedHashMap<>();
        private final Map<Attribute, List<AttributeModifierSpec>> modifiers = new LinkedHashMap<>();

        public Builder base(@NotNull Attribute attribute, double value) {
            this.baseValues.put(Objects.requireNonNull(attribute, "attribute"), value);
            return this;
        }

        public Builder modifier(@NotNull Attribute attribute, @NotNull AttributeModifierSpec modifier) {
            this.modifiers.computeIfAbsent(Objects.requireNonNull(attribute, "attribute"), ignored -> new ArrayList<>())
                    .add(Objects.requireNonNull(modifier, "modifier"));
            return this;
        }

        public EntityAttributeSpec build() {
            return new EntityAttributeSpec(this.baseValues, this.modifiers);
        }
    }
}
