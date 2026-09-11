package dev.willram.ramcore.region;

import dev.willram.ramcore.serialize.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Input for region rule evaluation.
 */
public record RegionQuery(
        @NotNull Position position,
        @NotNull RegionAction action,
        @Nullable Object subject,
        @NotNull Map<String, Object> metadata
) {

    @NotNull
    public static RegionQuery of(@NotNull Position position, @NotNull RegionAction action) {
        return new RegionQuery(position, action, null, Map.of());
    }

    @NotNull
    public RegionQuery withSubject(@Nullable Object subject) {
        return new RegionQuery(this.position, this.action, subject, this.metadata);
    }

    @NotNull
    public RegionQuery withMetadata(@NotNull Map<String, Object> metadata) {
        return new RegionQuery(this.position, this.action, this.subject, Map.copyOf(metadata));
    }
}
