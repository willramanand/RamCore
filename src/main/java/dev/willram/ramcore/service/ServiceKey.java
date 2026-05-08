package dev.willram.ramcore.service;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Typed identifier for a registered service.
 */
public final class ServiceKey<T> {
    private final String id;
    private final Class<T> type;

    private ServiceKey(@NotNull String id, @NotNull Class<T> type) {
        this.id = requireNonNull(id, "id");
        this.type = requireNonNull(type, "type");

        RamPreconditions.checkArgument(!id.isBlank(), "service id must not be blank", "Use a stable id such as 'config' or 'messages'.");
    }

    @NotNull
    public static <T> ServiceKey<T> of(@NotNull String id, @NotNull Class<T> type) {
        return new ServiceKey<>(id, type);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ServiceKey<?> that)) {
            return false;
        }

        return this.id.equals(that.id) && this.type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.id + " (" + this.type.getName() + ")";
    }
}
