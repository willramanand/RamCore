package dev.willram.ramcore.content;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Typed content id.
 */
public final class ContentKey<T> {
    private final ContentId id;
    private final Class<T> type;

    private ContentKey(@NotNull ContentId id, @NotNull Class<T> type) {
        this.id = requireNonNull(id, "id");
        this.type = requireNonNull(type, "type");
    }

    @NotNull
    public static <T> ContentKey<T> of(@NotNull ContentId id, @NotNull Class<T> type) {
        return new ContentKey<>(id, type);
    }

    @NotNull
    public static <T> ContentKey<T> of(@NotNull String namespace, @NotNull String value, @NotNull Class<T> type) {
        return of(ContentId.of(namespace, value), type);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    @NotNull
    public Class<T> type() {
        return this.type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ContentKey<?> that)) {
            return false;
        }

        return this.id.equals(that.id) && this.type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.id + " (" + this.type.getName() + ")";
    }
}
