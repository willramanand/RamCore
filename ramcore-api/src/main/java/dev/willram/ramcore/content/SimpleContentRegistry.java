package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class SimpleContentRegistry<T> implements ContentRegistry<T> {
    private final Class<T> type;
    private final Map<ContentId, ContentEntry<T>> entries = new LinkedHashMap<>();

    SimpleContentRegistry(@NotNull Class<T> type) {
        this.type = requireNonNull(type, "type");
    }

    @NotNull
    @Override
    public Class<T> type() {
        return this.type;
    }

    @NotNull
    @Override
    public ContentEntry<T> register(@NotNull String owner, @NotNull ContentKey<T> key, @NotNull T value) {
        requireNonNull(owner, "owner");
        requireNonNull(key, "key");
        requireNonNull(value, "value");

        RamPreconditions.checkArgument(!owner.isBlank(), "content owner must not be blank", "Use plugin name or module id.");
        RamPreconditions.checkArgument(
                key.type().equals(this.type),
                "content key type " + key.type().getName() + " does not match registry type " + this.type.getName(),
                "Register content with a key created for this registry type."
        );
        RamPreconditions.checkArgument(
                this.type.isInstance(value),
                "content value does not match registry type " + this.type.getName(),
                "Register an instance of " + this.type.getName() + "."
        );
        RamPreconditions.checkArgument(
                !this.entries.containsKey(key.id()),
                "content id already registered: " + key.id(),
                "Choose a different namespaced id or unregister the previous owner first."
        );

        ContentEntry<T> entry = new ContentEntry<>(key, owner, value);
        this.entries.put(key.id(), entry);
        return entry;
    }

    @NotNull
    @Override
    public Optional<T> get(@NotNull ContentId id) {
        ContentEntry<T> entry = this.entries.get(requireNonNull(id, "id"));
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    @NotNull
    @Override
    public T require(@NotNull ContentId id) {
        return get(id).orElseThrow(() -> RamPreconditions.misuse(
                "content id not registered: " + id,
                "Register content before lookup or check contains(id) first."
        ));
    }

    @Override
    public boolean contains(@NotNull ContentId id) {
        return this.entries.containsKey(requireNonNull(id, "id"));
    }

    @NotNull
    @Override
    public Set<ContentId> ids() {
        return Set.copyOf(this.entries.keySet());
    }

    @NotNull
    @Override
    public Collection<ContentEntry<T>> entries() {
        return List.copyOf(this.entries.values());
    }

    @Override
    public int unregisterOwner(@NotNull String owner) {
        requireNonNull(owner, "owner");
        int before = this.entries.size();
        this.entries.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
        return before - this.entries.size();
    }

    @Override
    public void clear() {
        this.entries.clear();
    }
}
