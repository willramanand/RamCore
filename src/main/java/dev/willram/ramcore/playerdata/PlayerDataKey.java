package dev.willram.ramcore.playerdata;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * Identifies one per-player value managed by a {@link PlayerDataService}.
 *
 * <p>{@code defaultFactory} builds the value for a player with nothing persisted yet. {@code snapshot}
 * copies the value on the player's thread right before it is handed to the async writer, so a
 * mutable {@code T} never has to be thread-safe: supply a copy constructor here. Immutable values
 * (records replaced through {@link PlayerDataService#set}) keep the identity default.</p>
 *
 * <p>Two keys are equal when their id and type match. Stability: experimental.</p>
 *
 * @param id             stable identifier, unique within a service
 * @param type           the value type
 * @param defaultFactory creates the value for a new player
 * @param snapshot       copies the value before an async save
 * @param <T>            the value type
 */
public record PlayerDataKey<T>(@NotNull String id, @NotNull Class<T> type, @NotNull Supplier<T> defaultFactory, @NotNull UnaryOperator<T> snapshot) {

    public PlayerDataKey {
        requireNonNull(id, "id");
        requireNonNull(type, "type");
        requireNonNull(defaultFactory, "defaultFactory");
        requireNonNull(snapshot, "snapshot");
        RamPreconditions.checkArgument(!id.isBlank(), "player data key id must not be blank", "Use a stable id such as 'profile' or 'stats'.");
    }

    /**
     * A key whose values are handed to the async writer as-is. Use for immutable values.
     *
     * @param id             stable identifier
     * @param type           the value type
     * @param defaultFactory creates the value for a new player
     * @param <T>            the value type
     * @return the key
     */
    @NotNull
    public static <T> PlayerDataKey<T> of(@NotNull String id, @NotNull Class<T> type, @NotNull Supplier<T> defaultFactory) {
        return new PlayerDataKey<>(id, type, defaultFactory, UnaryOperator.identity());
    }

    /**
     * A key whose values are copied on the player's thread before every async save.
     *
     * @param id             stable identifier
     * @param type           the value type
     * @param defaultFactory creates the value for a new player
     * @param snapshot       copy function, for example a copy constructor
     * @param <T>            the value type
     * @return the key
     */
    @NotNull
    public static <T> PlayerDataKey<T> of(@NotNull String id, @NotNull Class<T> type, @NotNull Supplier<T> defaultFactory, @NotNull UnaryOperator<T> snapshot) {
        return new PlayerDataKey<>(id, type, defaultFactory, snapshot);
    }

    /**
     * Builds the value for a player with nothing persisted.
     *
     * @return a fresh default value
     */
    @NotNull
    public T newDefault() {
        return requireNonNull(this.defaultFactory.get(), "defaultFactory returned null for key " + this.id);
    }

    /**
     * Applies the snapshot function.
     *
     * @param value the live value
     * @return the copy handed to the async writer
     */
    @NotNull
    public T copy(@NotNull T value) {
        return requireNonNull(this.snapshot.apply(value), "snapshot returned null for key " + this.id);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof PlayerDataKey<?> that && this.id.equals(that.id) && this.type.equals(that.type));
    }

    @Override
    public int hashCode() {
        return 31 * this.id.hashCode() + this.type.hashCode();
    }

    @Override
    public String toString() {
        return this.id + " (" + this.type.getName() + ")";
    }
}
