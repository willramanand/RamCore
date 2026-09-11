package dev.willram.ramcore.path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Terminal result for a path task.
 */
public record PathTaskResult(
        @NotNull PathStatus status,
        @NotNull PathProgress progress,
        @Nullable PathFailureReason failureReason,
        @Nullable PathCancelReason cancelReason,
        @NotNull String message
) {
    public PathTaskResult {
        status = Objects.requireNonNull(status, "status");
        progress = Objects.requireNonNull(progress, "progress");
        message = Objects.requireNonNull(message, "message");
    }

    public Optional<PathFailureReason> failureReasonOptional() {
        return Optional.ofNullable(this.failureReason);
    }

    public Optional<PathCancelReason> cancelReasonOptional() {
        return Optional.ofNullable(this.cancelReason);
    }

    public boolean successful() {
        return this.status == PathStatus.COMPLETE;
    }
}
