package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

/**
 * Owner-tracked resource-pack asset registration.
 */
public record ResourcePackAssetEntry(
        @NotNull String owner,
        @NotNull ResourcePackAsset asset
) {
}
