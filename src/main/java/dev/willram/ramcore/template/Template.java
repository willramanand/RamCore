package dev.willram.ramcore.template;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Typed template with optional parent inheritance.
 */
public record Template<T>(
        @NotNull ContentKey<T> key,
        @Nullable ContentId parent,
        @NotNull T value
) {

    @NotNull
    public static <T> Template<T> of(@NotNull ContentKey<T> key, @NotNull T value) {
        return new Template<>(key, null, value);
    }

    @NotNull
    public static <T> Template<T> extending(@NotNull ContentKey<T> key, @NotNull ContentId parent, @NotNull T value) {
        return new Template<>(key, parent, value);
    }

    public boolean hasParent() {
        return this.parent != null;
    }
}
