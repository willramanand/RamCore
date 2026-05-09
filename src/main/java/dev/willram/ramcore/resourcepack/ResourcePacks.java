package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * Facade for resource-pack helper APIs.
 */
public final class ResourcePacks {

    @NotNull
    public static ResourcePackAssetId id(@NotNull String namespace, @NotNull String path) {
        return ResourcePackAssetId.of(namespace, path);
    }

    @NotNull
    public static ResourcePackAssetId id(@NotNull String id) {
        return ResourcePackAssetId.parse(id);
    }

    @NotNull
    public static ResourcePackAssetRegistry registry() {
        return ResourcePackAssetRegistry.create();
    }

    @NotNull
    public static ResourcePackPromptTracker tracker() {
        return ResourcePackPromptTracker.create();
    }

    @NotNull
    public static ResourcePackPrompt.Builder prompt(@NotNull URI uri, @NotNull String sha1Hex) {
        return ResourcePackPrompt.builder(uri, sha1Hex);
    }

    private ResourcePacks() {
    }
}
