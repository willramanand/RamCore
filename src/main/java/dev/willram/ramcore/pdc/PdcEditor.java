package dev.willram.ramcore.pdc;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Typed mutable editor over a persistent data container.
 */
public final class PdcEditor {
    private final PersistentDataContainer container;

    private PdcEditor(@NotNull PersistentDataContainer container) {
        this.container = requireNonNull(container, "container");
    }

    @NotNull
    public static PdcEditor of(@NotNull PersistentDataContainer container) {
        return new PdcEditor(container);
    }

    @NotNull
    public PdcView view() {
        return PdcView.of(this.container);
    }

    public <P, C> boolean has(@NotNull PdcKey<P, C> key) {
        return view().has(key);
    }

    @NotNull
    public <P, C> java.util.Optional<C> get(@NotNull PdcKey<P, C> key) {
        return view().get(key);
    }

    @NotNull
    public <P, C> C getOrDefault(@NotNull PdcKey<P, C> key, @NotNull C defaultValue) {
        return view().getOrDefault(key, defaultValue);
    }

    @NotNull
    public <P, C> C getOrDefault(@NotNull PdcKey<P, C> key) {
        return view().getOrDefault(key);
    }

    @NotNull
    public <P, C> PdcEditor set(@NotNull PdcKey<P, C> key, @NotNull C value) {
        requireNonNull(key, "key");
        this.container.set(key.key(), key.type(), requireNonNull(value, "value"));
        return this;
    }

    @NotNull
    public <P, C> PdcEditor setIfPresent(@NotNull PdcKey<P, C> key, @NotNull java.util.Optional<? extends C> value) {
        requireNonNull(value, "value").ifPresentOrElse(v -> set(key, v), () -> remove(key));
        return this;
    }

    @NotNull
    public PdcEditor remove(@NotNull PdcKey<?, ?> key) {
        this.container.remove(requireNonNull(key, "key").key());
        return this;
    }

    @NotNull
    public PdcEditor remove(@NotNull NamespacedKey key) {
        this.container.remove(requireNonNull(key, "key"));
        return this;
    }

    @NotNull
    public PersistentDataContainer container() {
        return this.container;
    }
}
