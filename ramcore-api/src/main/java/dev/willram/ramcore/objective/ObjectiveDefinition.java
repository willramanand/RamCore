package dev.willram.ramcore.objective;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Reusable objective definition.
 */
public final class ObjectiveDefinition {
    private final ContentId id;
    private final List<ObjectiveTask> tasks;
    private final boolean chained;
    private final boolean hidden;

    private ObjectiveDefinition(@NotNull Builder builder) {
        this.id = builder.id;
        this.tasks = List.copyOf(builder.tasks);
        this.chained = builder.chained;
        this.hidden = builder.hidden;
        RamPreconditions.checkArgument(!this.tasks.isEmpty(), "objective must contain at least one task", "Add one or more ObjectiveTask entries.");
    }

    @NotNull
    public static Builder builder(@NotNull ContentId id) {
        return new Builder(id);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    @NotNull
    public List<ObjectiveTask> tasks() {
        return this.tasks;
    }

    public boolean chained() {
        return this.chained;
    }

    public boolean hidden() {
        return this.hidden;
    }

    @NotNull
    public ObjectiveTask task(@NotNull String taskId) {
        return this.tasks.stream()
                .filter(task -> task.id().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown objective task: " + taskId));
    }

    public static final class Builder {
        private final ContentId id;
        private final List<ObjectiveTask> tasks = new ArrayList<>();
        private boolean chained;
        private boolean hidden;

        private Builder(@NotNull ContentId id) {
            this.id = requireNonNull(id, "id");
        }

        @NotNull
        public Builder task(@NotNull ObjectiveTask task) {
            this.tasks.add(requireNonNull(task, "task"));
            return this;
        }

        @NotNull
        public Builder chained(boolean chained) {
            this.chained = chained;
            return this;
        }

        @NotNull
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        @NotNull
        public ObjectiveDefinition build() {
            return new ObjectiveDefinition(this);
        }
    }
}
