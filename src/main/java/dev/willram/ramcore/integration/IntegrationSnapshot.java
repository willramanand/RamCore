package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime view of one integration.
 */
public record IntegrationSnapshot(
        @NotNull IntegrationDescriptor descriptor,
        @NotNull IntegrationStatus status,
        @Nullable String version,
        @NotNull String message
) {

    public boolean available() {
        return this.status == IntegrationStatus.AVAILABLE;
    }
}
