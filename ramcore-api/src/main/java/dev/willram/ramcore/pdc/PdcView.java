package dev.willram.ramcore.pdc;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Typed read-only view over a persistent data container.
 */
public final class PdcView {
    private final PersistentDataContainerView container;

    private PdcView(@NotNull PersistentDataContainerView container) {
        this.container = requireNonNull(container, "container");
    }

    @NotNull
    public static PdcView of(@NotNull PersistentDataContainerView container) {
        return new PdcView(container);
    }

    public <P, C> boolean has(@NotNull PdcKey<P, C> key) {
        requireNonNull(key, "key");
        return this.container.has(key.key(), key.type());
    }

    @NotNull
    public <P, C> Optional<C> get(@NotNull PdcKey<P, C> key) {
        requireNonNull(key, "key");
        return Optional.ofNullable(this.container.get(key.key(), key.type()));
    }

    @NotNull
    public <P, C> C getOrDefault(@NotNull PdcKey<P, C> key, @NotNull C defaultValue) {
        requireNonNull(key, "key");
        requireNonNull(defaultValue, "defaultValue");
        return this.container.getOrDefault(key.key(), key.type(), defaultValue);
    }

    @NotNull
    public <P, C> C getOrDefault(@NotNull PdcKey<P, C> key) {
        requireNonNull(key, "key");
        if (!key.hasDefault()) {
            throw new IllegalStateException("PDC key " + key + " does not define a default value.");
        }
        return getOrDefault(key, key.defaultValue());
    }

    @NotNull
    public <P, C> C require(@NotNull PdcKey<P, C> key) {
        return get(key).orElseThrow(() -> new IllegalStateException("Missing required PDC value: " + key));
    }

    public boolean has(@NotNull NamespacedKey key) {
        return this.container.has(requireNonNull(key, "key"));
    }

    public boolean isEmpty() {
        return this.container.isEmpty();
    }

    @NotNull
    public Set<NamespacedKey> keys() {
        return this.container.getKeys();
    }

    @NotNull
    public PersistentDataContainerView container() {
        return this.container;
    }
}
