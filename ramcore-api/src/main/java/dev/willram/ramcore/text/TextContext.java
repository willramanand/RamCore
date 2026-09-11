package dev.willram.ramcore.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable typed placeholder value set.
 */
public final class TextContext {
    private static final TextContext EMPTY = new TextContext(Map.of());

    private final Map<TextPlaceholder<?>, Object> values;

    private TextContext(@NotNull Map<TextPlaceholder<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    @NotNull
    public static TextContext empty() {
        return EMPTY;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public <T> Optional<T> get(@NotNull TextPlaceholder<T> placeholder) {
        Objects.requireNonNull(placeholder, "placeholder");
        Object value = this.values.get(placeholder);
        return value == null ? Optional.empty() : Optional.of(placeholder.cast(value));
    }

    @NotNull
    public TextContext merge(@NotNull TextContext other) {
        Objects.requireNonNull(other, "other");
        if (this.values.isEmpty()) {
            return other;
        }
        if (other.values.isEmpty()) {
            return this;
        }

        Map<TextPlaceholder<?>, Object> merged = new LinkedHashMap<>(this.values);
        merged.putAll(other.values);
        return new TextContext(merged);
    }

    @NotNull
    public TagResolver resolver() {
        return this.values.entrySet().stream()
                .map(entry -> entry.getKey().resolver(entry.getValue()))
                .collect(TagResolver.toTagResolver());
    }

    public static final class Builder {
        private final Map<TextPlaceholder<?>, Object> values = new LinkedHashMap<>();

        @NotNull
        public <T> Builder put(@NotNull TextPlaceholder<T> placeholder, @NotNull T value) {
            Objects.requireNonNull(placeholder, "placeholder");
            this.values.put(placeholder, placeholder.cast(value));
            return this;
        }

        @NotNull
        public Builder parsed(@NotNull String name, @NotNull Object value) {
            return put(TextPlaceholder.parsed(name, Object.class, String::valueOf), value);
        }

        @NotNull
        public Builder unparsed(@NotNull String name, @NotNull Object value) {
            return put(TextPlaceholder.unparsed(name, Object.class, String::valueOf), value);
        }

        @NotNull
        public Builder component(@NotNull String name, @NotNull ComponentLike value) {
            return put(TextPlaceholder.component(name, Component.class), value.asComponent());
        }

        @NotNull
        public TextContext build() {
            return this.values.isEmpty() ? EMPTY : new TextContext(this.values);
        }
    }
}
