package dev.willram.ramcore.pdc;

import dev.willram.ramcore.data.NamespacedKeys;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public final class PDCs {

    @NotNull
    public static NamespacedKey key(@NotNull String key) {
        return NamespacedKeys.create(PdcKey.validateKey(key));
    }

    @NotNull
    public static NamespacedKey key(@NotNull String namespace, @NotNull String key) {
        return PdcKey.namespaced(namespace, key);
    }

    @NotNull
    public static <P, C> PdcKey<P, C> typedKey(@NotNull String key, @NotNull PersistentDataType<P, C> type) {
        return PdcKey.of(key, type);
    }

    @NotNull
    public static <P, C> PdcKey<P, C> typedKey(@NotNull String namespace, @NotNull String key, @NotNull PersistentDataType<P, C> type) {
        return PdcKey.of(namespace, key, type);
    }

    @NotNull
    public static <T> PersistentDataType<String, T> jsonType(@NotNull Class<T> type) {
        return new PdcJsonDataType<>(type);
    }

    @NotNull
    public static <T> PersistentDataType<String, T> objectType(@NotNull Class<T> type, @NotNull PdcObjectCodec<T> codec) {
        return new PdcObjectDataType<>(type, codec);
    }

    @NotNull
    public static PdcView view(@NotNull PersistentDataHolder holder) {
        return PdcView.of(holder.getPersistentDataContainer());
    }

    @NotNull
    public static PdcView view(@NotNull PersistentDataContainerView container) {
        return PdcView.of(container);
    }

    @NotNull
    public static PdcEditor edit(@NotNull PersistentDataHolder holder) {
        return PdcEditor.of(holder.getPersistentDataContainer());
    }

    @NotNull
    public static PdcEditor edit(@NotNull PersistentDataContainer container) {
        return PdcEditor.of(container);
    }

    @Nullable
    public static <T, Z> Z get(@NotNull PersistentDataHolder holder, @NotNull String key, @NotNull PersistentDataType<T, Z> type) {
        return holder.getPersistentDataContainer().get(key(key), type);
    }

    @NotNull
    public static <P, C> Optional<C> get(@NotNull PersistentDataHolder holder, @NotNull PdcKey<P, C> key) {
        return view(holder).get(key);
    }

    @NotNull
    public static <P, C> Optional<C> get(@NotNull PersistentDataContainerView container, @NotNull PdcKey<P, C> key) {
        return view(container).get(key);
    }

    @NotNull
    public static <P, C> C getOrDefault(@NotNull PersistentDataHolder holder, @NotNull PdcKey<P, C> key, @NotNull C defaultValue) {
        return view(holder).getOrDefault(key, defaultValue);
    }

    @NotNull
    public static <P, C> C getOrDefault(@NotNull PersistentDataContainerView container, @NotNull PdcKey<P, C> key, @NotNull C defaultValue) {
        return view(container).getOrDefault(key, defaultValue);
    }

    @NotNull
    public static <P, C> C getOrDefault(@NotNull PersistentDataHolder holder, @NotNull PdcKey<P, C> key) {
        return view(holder).getOrDefault(key);
    }

    public static <T, Z> void set(@NotNull PersistentDataHolder holder, @NotNull String key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        holder.getPersistentDataContainer().set(key(key), type, value);
    }

    public static <P, C> void set(@NotNull PersistentDataHolder holder, @NotNull PdcKey<P, C> key, @NotNull C value) {
        edit(holder).set(key, value);
    }

    public static <P, C> void set(@NotNull PersistentDataContainer container, @NotNull PdcKey<P, C> key, @NotNull C value) {
        edit(container).set(key, value);
    }

    public static boolean has(@NotNull PersistentDataHolder holder, @NotNull String key) {
        return holder.getPersistentDataContainer().has(key(key));
    }

    public static <P, C> boolean has(@NotNull PersistentDataHolder holder, @NotNull PdcKey<P, C> key) {
        return view(holder).has(key);
    }

    public static <P, C> boolean has(@NotNull PersistentDataContainerView container, @NotNull PdcKey<P, C> key) {
        return view(container).has(key);
    }

    public static void remove(@NotNull PersistentDataHolder holder, @NotNull String key) {
        holder.getPersistentDataContainer().remove(key(key));
    }

    public static void remove(@NotNull PersistentDataHolder holder, @NotNull PdcKey<?, ?> key) {
        edit(holder).remove(key);
    }

    private PDCs() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
