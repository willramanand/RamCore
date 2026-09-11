package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.RamPreconditions;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Namespaced resource-pack asset id using Minecraft's namespace:path shape.
 */
public final class ResourcePackAssetId implements Comparable<ResourcePackAssetId> {
    private final String namespace;
    private final String path;

    private ResourcePackAssetId(@NotNull String namespace, @NotNull String path) {
        this.namespace = validateNamespace(namespace);
        this.path = validatePath(path);
    }

    @NotNull
    public static ResourcePackAssetId of(@NotNull String namespace, @NotNull String path) {
        return new ResourcePackAssetId(namespace, path);
    }

    @NotNull
    public static ResourcePackAssetId parse(@NotNull String id) {
        requireNonNull(id, "id");
        int separator = id.indexOf(':');
        RamPreconditions.checkArgument(
                separator > 0 && separator < id.length() - 1,
                "resource-pack asset id must use namespace:path form",
                "Use an id such as 'example:item/fire_sword'."
        );
        return of(id.substring(0, separator), id.substring(separator + 1));
    }

    @NotNull
    public String namespace() {
        return this.namespace;
    }

    @NotNull
    public String path() {
        return this.path;
    }

    @NotNull
    public NamespacedKey asBukkitKey() {
        return new NamespacedKey(this.namespace, this.path);
    }

    @Override
    public int compareTo(@NotNull ResourcePackAssetId other) {
        int namespaceCompare = this.namespace.compareTo(other.namespace);
        return namespaceCompare != 0 ? namespaceCompare : this.path.compareTo(other.path);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ResourcePackAssetId that)) {
            return false;
        }

        return this.namespace.equals(that.namespace) && this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = this.namespace.hashCode();
        result = 31 * result + this.path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }

    private static String validateNamespace(String namespace) {
        requireNonNull(namespace, "namespace");
        String trimmed = namespace.trim();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_.-]+"),
                "resource-pack namespace contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, or dashes."
        );
        return trimmed;
    }

    private static String validatePath(String path) {
        requireNonNull(path, "path");
        String trimmed = path.trim();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_./-]+"),
                "resource-pack asset path contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, or slashes."
        );
        return trimmed;
    }
}
