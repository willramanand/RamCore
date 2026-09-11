package dev.willram.ramcore.reward;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Context passed through reward validation, preview, and execution.
 */
public record RewardContext(
        @NotNull String scope,
        @Nullable Object subject,
        @NotNull Map<String, Object> metadata
) {

    @NotNull
    public static RewardContext of(@NotNull String scope) {
        return new RewardContext(scope, null, Map.of());
    }

    @NotNull
    public RewardContext withSubject(@Nullable Object subject) {
        return new RewardContext(this.scope, subject, this.metadata);
    }

    @NotNull
    public RewardContext withMetadata(@NotNull Map<String, Object> metadata) {
        return new RewardContext(this.scope, this.subject, Map.copyOf(metadata));
    }
}
