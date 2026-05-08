package dev.willram.ramcore.objective;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * One measurable step inside an objective.
 */
public final class ObjectiveTask {
    private final String id;
    private final ObjectiveAction action;
    private final String target;
    private final long required;
    private boolean hidden;

    private ObjectiveTask(@NotNull String id, @NotNull ObjectiveAction action, @NotNull String target, long required) {
        this.id = validate(id, "objective task id");
        this.action = requireNonNull(action, "action");
        this.target = validate(target, "objective task target");
        RamPreconditions.checkArgument(required > 0, "objective task required amount must be positive", "Use at least 1.");
        this.required = required;
    }

    @NotNull
    public static ObjectiveTask of(@NotNull String id, @NotNull ObjectiveAction action, @NotNull String target, long required) {
        return new ObjectiveTask(id, action, target, required);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public ObjectiveAction action() {
        return this.action;
    }

    @NotNull
    public String target() {
        return this.target;
    }

    public long required() {
        return this.required;
    }

    public boolean hidden() {
        return this.hidden;
    }

    @NotNull
    public ObjectiveTask hidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public boolean matches(@NotNull ObjectiveEvent event) {
        requireNonNull(event, "event");
        return this.action == event.action() && (this.target.equals("*") || this.target.equals(event.target()));
    }

    @NotNull
    private static String validate(@NotNull String value, @NotNull String subject) {
        requireNonNull(value, subject);
        String trimmed = value.trim().toLowerCase();
        RamPreconditions.checkArgument(
                !trimmed.isEmpty() && trimmed.matches("[a-z0-9_.:-]+|\\*"),
                subject + " contains invalid characters",
                "Use lowercase letters, numbers, underscores, dots, dashes, colons, or '*'."
        );
        return trimmed;
    }
}
