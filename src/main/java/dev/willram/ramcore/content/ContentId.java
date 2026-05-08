package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Stable namespaced content id.
 */
public final class ContentId implements Comparable<ContentId> {
    private final String namespace;
    private final String value;

    private ContentId(@NotNull String namespace, @NotNull String value) {
        this.namespace = validatePart(namespace, "content namespace");
        this.value = validatePart(value, "content id");
    }

    @NotNull
    public static ContentId of(@NotNull String namespace, @NotNull String value) {
        return new ContentId(namespace, value);
    }

    @NotNull
    public static ContentId parse(@NotNull String id) {
        requireNonNull(id, "id");
        int separator = id.indexOf(':');
        RamPreconditions.checkArgument(
                separator > 0 && separator < id.length() - 1,
                "content id must use namespace:value form",
                "Use an id such as 'myplugin:fire_sword'."
        );
        return of(id.substring(0, separator), id.substring(separator + 1));
    }

    @NotNull
    public String namespace() {
        return this.namespace;
    }

    @NotNull
    public String value() {
        return this.value;
    }

    @Override
    public int compareTo(@NotNull ContentId other) {
        int namespaceCompare = this.namespace.compareTo(other.namespace);
        return namespaceCompare != 0 ? namespaceCompare : this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ContentId that)) {
            return false;
        }

        return this.namespace.equals(that.namespace) && this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = this.namespace.hashCode();
        result = 31 * result + this.value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.value;
    }

    private static String validatePart(String value, String subject) {
        requireNonNull(value, subject);
        String trimmed = value.trim();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_.-]+"),
                subject + " contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, or dashes."
        );
        return trimmed;
    }
}
