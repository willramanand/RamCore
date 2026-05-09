package dev.willram.ramcore.menu;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Per-viewer mutable menu state.
 */
public final class MenuState {
    private final Map<String, Object> values = new LinkedHashMap<>();

    @NotNull
    public static MenuState create() {
        return new MenuState();
    }

    @NotNull
    public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
        requireNonNull(key, "key");
        requireNonNull(type, "type");
        Object value = this.values.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    @Nullable
    public Object get(@NotNull String key) {
        return this.values.get(requireNonNull(key, "key"));
    }

    @NotNull
    public <T> T getOrDefault(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        return get(key, type).orElse(defaultValue);
    }

    public int intValue(@NotNull String key, int defaultValue) {
        Object value = this.values.get(requireNonNull(key, "key"));
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    @NotNull
    public MenuState put(@NotNull String key, @Nullable Object value) {
        requireNonNull(key, "key");
        if (value == null) {
            this.values.remove(key);
        } else {
            this.values.put(key, value);
        }
        return this;
    }

    @Nullable
    public Object remove(@NotNull String key) {
        return this.values.remove(requireNonNull(key, "key"));
    }

    public boolean contains(@NotNull String key) {
        return this.values.containsKey(requireNonNull(key, "key"));
    }

    public void clear() {
        this.values.clear();
    }

    @NotNull
    public Map<String, Object> asMap() {
        return this.values;
    }

    @NotNull
    public Map<String, Object> snapshot() {
        return Map.copyOf(this.values);
    }
}
