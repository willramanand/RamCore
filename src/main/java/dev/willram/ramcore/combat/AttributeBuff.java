package dev.willram.ramcore.combat;

import dev.willram.ramcore.entity.EntityAttributeSpec;
import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Temporary attribute profile that restores previous values and replaced
 * modifiers when closed.
 */
public final class AttributeBuff implements Terminable {
    private final LivingEntity entity;
    private final EntityAttributeSpec spec;
    private final Map<Attribute, Double> previousBases = new LinkedHashMap<>();
    private final List<PreviousModifier> previousModifiers = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    private AttributeBuff(@NotNull LivingEntity entity, @NotNull EntityAttributeSpec spec) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    @NotNull
    public static AttributeBuff apply(@NotNull LivingEntity entity, @NotNull EntityAttributeSpec spec) {
        AttributeBuff buff = new AttributeBuff(entity, spec);
        buff.apply();
        return buff;
    }

    @NotNull
    public LivingEntity entity() {
        return this.entity;
    }

    private void apply() {
        for (Map.Entry<Attribute, Double> entry : this.spec.baseValues().entrySet()) {
            AttributeInstance instance = this.entity.getAttribute(entry.getKey());
            if (instance == null) {
                continue;
            }
            this.previousBases.put(entry.getKey(), instance.getBaseValue());
            instance.setBaseValue(entry.getValue());
        }
        for (Map.Entry<Attribute, List<AttributeModifierSpec>> entry : this.spec.modifiers().entrySet()) {
            AttributeInstance instance = this.entity.getAttribute(entry.getKey());
            if (instance == null) {
                continue;
            }
            for (AttributeModifierSpec spec : entry.getValue()) {
                AttributeModifier previous = instance.getModifier(spec.key());
                this.previousModifiers.add(new PreviousModifier(entry.getKey(), spec, previous));
                instance.removeModifier(spec.key());
                if (spec.transientModifier()) {
                    instance.addTransientModifier(spec.toBukkit());
                } else {
                    instance.addModifier(spec.toBukkit());
                }
            }
        }
    }

    @Override
    public void close() {
        if (this.closed.getAndSet(true)) {
            return;
        }
        for (PreviousModifier previous : this.previousModifiers) {
            AttributeInstance instance = this.entity.getAttribute(previous.attribute);
            if (instance == null) {
                continue;
            }
            instance.removeModifier(previous.spec.key());
            if (previous.previous != null) {
                instance.addModifier(previous.previous);
            }
        }
        for (Map.Entry<Attribute, Double> entry : this.previousBases.entrySet()) {
            AttributeInstance instance = this.entity.getAttribute(entry.getKey());
            if (instance != null) {
                instance.setBaseValue(entry.getValue());
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed.get();
    }

    private record PreviousModifier(
            @NotNull Attribute attribute,
            @NotNull AttributeModifierSpec spec,
            @Nullable AttributeModifier previous
    ) {
    }
}
