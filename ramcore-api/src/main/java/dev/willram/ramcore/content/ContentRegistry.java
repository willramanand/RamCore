package dev.willram.ramcore.content;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Typed content registry with namespaced ids and owner tracking.
 */
public interface ContentRegistry<T> extends AutoCloseable {

    @NotNull
    static <T> ContentRegistry<T> create(@NotNull Class<T> type) {
        return new SimpleContentRegistry<>(type);
    }

    @NotNull
    Class<T> type();

    @NotNull
    ContentEntry<T> register(@NotNull String owner, @NotNull ContentKey<T> key, @NotNull T value);

    @NotNull
    Optional<T> get(@NotNull ContentId id);

    @NotNull
    T require(@NotNull ContentId id);

    boolean contains(@NotNull ContentId id);

    @NotNull
    Set<ContentId> ids();

    @NotNull
    Collection<ContentEntry<T>> entries();

    int unregisterOwner(@NotNull String owner);

    void clear();

    @Override
    default void close() {
        clear();
    }
}
