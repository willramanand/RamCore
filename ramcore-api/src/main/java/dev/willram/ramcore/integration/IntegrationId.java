package dev.willram.ramcore.integration;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Stable id for an optional integration.
 */
public record IntegrationId(@NotNull String value) implements Comparable<IntegrationId> {

    public IntegrationId {
        requireNonNull(value, "value");
        value = value.trim().toLowerCase();
        RamPreconditions.checkArgument(
                !value.isEmpty() && value.matches("[a-z0-9_.:-]+"),
                "integration id contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, or colons."
        );
    }

    @NotNull
    public static IntegrationId of(@NotNull String value) {
        return new IntegrationId(value);
    }

    @Override
    public int compareTo(@NotNull IntegrationId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
