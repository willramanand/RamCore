package dev.willram.ramcore.text;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/**
 * Typed placeholder declaration for MiniMessage rendering.
 *
 * @param <T> value type accepted by this placeholder
 */
public final class TextPlaceholder<T> {
    private final String name;
    private final Class<T> type;
    private final TextPlaceholderMode mode;
    private final Function<? super T, String> stringifier;

    private TextPlaceholder(@NotNull String name, @NotNull Class<T> type, @NotNull TextPlaceholderMode mode,
                            @NotNull Function<? super T, String> stringifier) {
        this.name = RamPreconditions.notBlank(name, "placeholder name");
        this.type = Objects.requireNonNull(type, "type");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.stringifier = Objects.requireNonNull(stringifier, "stringifier");
        if (mode == TextPlaceholderMode.COMPONENT) {
            RamPreconditions.checkArgument(
                    ComponentLike.class.isAssignableFrom(type),
                    "component placeholder type must implement ComponentLike",
                    "Use TextPlaceholder.component(name, Component.class) or another ComponentLike type."
            );
        }
    }

    @NotNull
    public static <T> TextPlaceholder<T> parsed(@NotNull String name, @NotNull Class<T> type,
                                                @NotNull Function<? super T, String> stringifier) {
        return new TextPlaceholder<>(name, type, TextPlaceholderMode.PARSED, stringifier);
    }

    @NotNull
    public static <T> TextPlaceholder<T> unparsed(@NotNull String name, @NotNull Class<T> type,
                                                  @NotNull Function<? super T, String> stringifier) {
        return new TextPlaceholder<>(name, type, TextPlaceholderMode.UNPARSED, stringifier);
    }

    @NotNull
    public static <T extends ComponentLike> TextPlaceholder<T> component(@NotNull String name, @NotNull Class<T> type) {
        return new TextPlaceholder<>(name, type, TextPlaceholderMode.COMPONENT, value -> "");
    }

    @NotNull
    public String name() {
        return this.name;
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @NotNull
    public TextPlaceholderMode mode() {
        return this.mode;
    }

    @NotNull
    public T cast(@NotNull Object value) {
        return this.type.cast(Objects.requireNonNull(value, "value"));
    }

    @NotNull
    public TagResolver resolver(@NotNull Object value) {
        T typed = cast(value);
        return switch (this.mode) {
            case PARSED -> Placeholder.parsed(this.name, this.stringifier.apply(typed));
            case UNPARSED -> Placeholder.unparsed(this.name, this.stringifier.apply(typed));
            case COMPONENT -> Placeholder.component(this.name, (ComponentLike) typed);
        };
    }
}
