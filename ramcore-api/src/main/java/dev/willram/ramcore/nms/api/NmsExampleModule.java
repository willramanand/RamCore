package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Manual server-test example descriptor for an advanced feature.
 */
public record NmsExampleModule(
        @NotNull String id,
        @NotNull NmsCapability capability,
        @NotNull String description,
        @NotNull String manualCheck
) {

    public NmsExampleModule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(manualCheck, "manualCheck");
        if (id.isBlank()) {
            throw new IllegalArgumentException("example id must not be blank");
        }
    }
}
