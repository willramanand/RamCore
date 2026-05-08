package dev.willram.ramcore.party;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Stable party identifier.
 */
public final class PartyId implements Comparable<PartyId> {
    private final String value;

    private PartyId(@NotNull String value) {
        requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase();
        RamPreconditions.checkArgument(
                !normalized.isEmpty() && normalized.matches("[a-z0-9_.:-]+"),
                "party id contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, or colons."
        );
        this.value = normalized;
    }

    @NotNull
    public static PartyId of(@NotNull String value) {
        return new PartyId(value);
    }

    @NotNull
    public static PartyId random() {
        return of(UUID.randomUUID().toString());
    }

    @NotNull
    public String value() {
        return this.value;
    }

    @Override
    public int compareTo(@NotNull PartyId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PartyId that && this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }
}
