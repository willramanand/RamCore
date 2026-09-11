package dev.willram.ramcore.combat;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Typed value object for a Bukkit attribute modifier.
 */
public record AttributeModifierSpec(
        @NotNull NamespacedKey key,
        double amount,
        @NotNull AttributeModifier.Operation operation,
        boolean transientModifier
) {
    public AttributeModifierSpec {
        key = Objects.requireNonNull(key, "key");
        operation = Objects.requireNonNull(operation, "operation");
    }

    @NotNull
    public static Builder builder(@NotNull NamespacedKey key) {
        return new Builder(key);
    }

    @NotNull
    public AttributeModifier toBukkit() {
        return new AttributeModifier(this.key, this.amount, this.operation);
    }

    public static final class Builder {
        private final NamespacedKey key;
        private double amount;
        private AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
        private boolean transientModifier;

        private Builder(NamespacedKey key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder operation(@NotNull AttributeModifier.Operation operation) {
            this.operation = Objects.requireNonNull(operation, "operation");
            return this;
        }

        public Builder addNumber(double amount) {
            return amount(amount).operation(AttributeModifier.Operation.ADD_NUMBER);
        }

        public Builder addScalar(double amount) {
            return amount(amount).operation(AttributeModifier.Operation.ADD_SCALAR);
        }

        public Builder multiplyScalar(double amount) {
            return amount(amount).operation(AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        }

        public Builder transientModifier(boolean transientModifier) {
            this.transientModifier = transientModifier;
            return this;
        }

        public AttributeModifierSpec build() {
            return new AttributeModifierSpec(this.key, this.amount, this.operation, this.transientModifier);
        }
    }
}
