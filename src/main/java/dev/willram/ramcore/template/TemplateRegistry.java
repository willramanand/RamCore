package dev.willram.ramcore.template;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Typed template registry with parent validation and resolution.
 */
public interface TemplateRegistry<T> extends AutoCloseable {

    @NotNull
    static <T> TemplateRegistry<T> create(@NotNull Class<T> type, @NotNull TemplateComposer<T> composer) {
        return new SimpleTemplateRegistry<>(type, composer);
    }

    @NotNull
    Class<T> type();

    @NotNull
    TemplateEntry<T> register(@NotNull String owner, @NotNull Template<T> template);

    @NotNull
    default TemplateEntry<T> register(@NotNull String owner, @NotNull ContentKey<T> key, @NotNull T value) {
        return register(owner, Template.of(key, value));
    }

    @NotNull
    Optional<Template<T>> get(@NotNull ContentId id);

    @NotNull
    Template<T> require(@NotNull ContentId id);

    @NotNull
    T resolve(@NotNull ContentId id);

    @NotNull
    Set<ContentId> ids();

    @NotNull
    Collection<TemplateEntry<T>> entries();

    void validate();

    int unregisterOwner(@NotNull String owner);

    void clear();

    @Override
    default void close() {
        clear();
    }
}
