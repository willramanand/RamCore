package dev.willram.ramcore.objective;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Input event used to advance objective progress.
 */
public record ObjectiveEvent(
        @NotNull ObjectiveSubject subject,
        @NotNull ObjectiveAction action,
        @NotNull String target,
        long amount,
        @NotNull Map<String, Object> metadata
) {

    public ObjectiveEvent {
        requireNonNull(subject, "subject");
        requireNonNull(action, "action");
        requireNonNull(target, "target");
        requireNonNull(metadata, "metadata");
        RamPreconditions.checkArgument(amount > 0, "objective event amount must be positive", "Use at least 1.");
        target = target.trim().toLowerCase();
        RamPreconditions.checkArgument(!target.isEmpty(), "objective event target is empty", "Use a target id or '*'.");
        metadata = Map.copyOf(metadata);
    }

    @NotNull
    public static ObjectiveEvent of(@NotNull ObjectiveSubject subject, @NotNull ObjectiveAction action, @NotNull String target) {
        return new ObjectiveEvent(subject, action, target, 1L, Map.of());
    }

    @NotNull
    public ObjectiveEvent amount(long amount) {
        return new ObjectiveEvent(this.subject, this.action, this.target, amount, this.metadata);
    }

    @NotNull
    public ObjectiveEvent withMetadata(@NotNull Map<String, Object> metadata) {
        return new ObjectiveEvent(this.subject, this.action, this.target, this.amount, metadata);
    }
}
