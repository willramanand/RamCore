package dev.willram.ramcore.pdc;

import dev.willram.ramcore.exception.RamPreconditions;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Typed persistent data key.
 */
public final class PdcKey<P, C> {
    private final NamespacedKey key;
    private final PersistentDataType<P, C> type;
    private final C defaultValue;

    private PdcKey(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type, @Nullable C defaultValue) {
        this.key = requireNonNull(key, "key");
        this.type = requireNonNull(type, "type");
        this.defaultValue = defaultValue;
    }

    @NotNull
    public static <P, C> PdcKey<P, C> of(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type) {
        return new PdcKey<>(key, type, null);
    }

    @NotNull
    public static <P, C> PdcKey<P, C> of(@NotNull String namespace, @NotNull String key, @NotNull PersistentDataType<P, C> type) {
        return of(namespaced(namespace, key), type);
    }

    @NotNull
    public static <P, C> PdcKey<P, C> of(@NotNull String key, @NotNull PersistentDataType<P, C> type) {
        return of(PDCs.key(key), type);
    }

    @NotNull
    public PdcKey<P, C> defaultValue(@NotNull C defaultValue) {
        return new PdcKey<>(this.key, this.type, requireNonNull(defaultValue, "defaultValue"));
    }

    @NotNull
    public NamespacedKey key() {
        return this.key;
    }

    @NotNull
    public PersistentDataType<P, C> type() {
        return this.type;
    }

    @Nullable
    public C defaultValue() {
        return this.defaultValue;
    }

    public boolean hasDefault() {
        return this.defaultValue != null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PdcKey<?, ?> that)) {
            return false;
        }

        return this.key.equals(that.key) && this.type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.type);
    }

    @Override
    public String toString() {
        return this.key.toString();
    }

    @NotNull
    static NamespacedKey namespaced(@NotNull String namespace, @NotNull String key) {
        String ns = validateNamespace(namespace);
        String value = validateKey(key);
        return new NamespacedKey(ns, value);
    }

    @NotNull
    static String validateNamespace(String namespace) {
        requireNonNull(namespace, "namespace");
        String trimmed = namespace.trim();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_.-]+"),
                "PDC namespace contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, or dashes."
        );
        return trimmed;
    }

    @NotNull
    static String validateKey(String key) {
        requireNonNull(key, "key");
        String trimmed = key.trim();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_./-]+"),
                "PDC key contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, or slashes."
        );
        return trimmed;
    }
}
