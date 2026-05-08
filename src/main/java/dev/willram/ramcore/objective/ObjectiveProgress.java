package dev.willram.ramcore.objective;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Mutable progress for one subject/objective pair.
 */
public final class ObjectiveProgress {
    private final ObjectiveSubject subject;
    private final ContentId objectiveId;
    private final Map<String, Long> amounts = new LinkedHashMap<>();

    ObjectiveProgress(@NotNull ObjectiveSubject subject, @NotNull ContentId objectiveId) {
        this.subject = requireNonNull(subject, "subject");
        this.objectiveId = requireNonNull(objectiveId, "objectiveId");
    }

    @NotNull
    public ObjectiveSubject subject() {
        return this.subject;
    }

    @NotNull
    public ContentId objectiveId() {
        return this.objectiveId;
    }

    public long current(@NotNull String taskId) {
        return this.amounts.getOrDefault(requireNonNull(taskId, "taskId"), 0L);
    }

    public boolean completed(@NotNull ObjectiveTask task) {
        return current(task.id()) >= task.required();
    }

    public boolean completed(@NotNull ObjectiveDefinition definition) {
        return definition.tasks().stream().allMatch(this::completed);
    }

    @NotNull
    public List<ObjectiveTaskProgress> tasks(@NotNull ObjectiveDefinition definition) {
        return definition.tasks().stream()
                .map(task -> new ObjectiveTaskProgress(task.id(), current(task.id()), task.required(), completed(task), task.hidden() || definition.hidden()))
                .toList();
    }

    @NotNull
    public Map<String, Long> snapshot() {
        return Map.copyOf(this.amounts);
    }

    void reset() {
        this.amounts.clear();
    }

    long advance(@NotNull ObjectiveTask task, long amount) {
        requireNonNull(task, "task");
        long before = current(task.id());
        if (before >= task.required()) {
            return before;
        }
        long after = Math.min(task.required(), before + amount);
        this.amounts.put(task.id(), after);
        return after;
    }
}
