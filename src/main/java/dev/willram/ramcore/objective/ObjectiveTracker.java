package dev.willram.ramcore.objective;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * In-memory objective definition and progress tracker.
 */
public final class ObjectiveTracker {
    private final Map<ContentId, ObjectiveDefinition> definitions = new LinkedHashMap<>();
    private final Map<ObjectiveSubject, Map<ContentId, ObjectiveProgress>> progress = new LinkedHashMap<>();
    private final List<ObjectiveProgressListener> listeners = new ArrayList<>();

    @NotNull
    public ObjectiveTracker register(@NotNull ObjectiveDefinition definition) {
        requireNonNull(definition, "definition");
        RamPreconditions.checkArgument(!this.definitions.containsKey(definition.id()), "objective already registered", "Use a unique objective id.");
        this.definitions.put(definition.id(), definition);
        return this;
    }

    @NotNull
    public ObjectiveTracker listener(@NotNull ObjectiveProgressListener listener) {
        this.listeners.add(requireNonNull(listener, "listener"));
        return this;
    }

    @NotNull
    public Optional<ObjectiveDefinition> definition(@NotNull ContentId id) {
        return Optional.ofNullable(this.definitions.get(requireNonNull(id, "id")));
    }

    @NotNull
    public List<ObjectiveDefinition> definitions() {
        return List.copyOf(this.definitions.values());
    }

    @NotNull
    public ObjectiveProgress progress(@NotNull ObjectiveSubject subject, @NotNull ContentId objectiveId) {
        requireNonNull(subject, "subject");
        requireNonNull(objectiveId, "objectiveId");
        RamPreconditions.checkArgument(this.definitions.containsKey(objectiveId), "objective is not registered", "Register the objective before requesting progress.");
        return this.progress
                .computeIfAbsent(subject, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(objectiveId, ignored -> new ObjectiveProgress(subject, objectiveId));
    }

    @NotNull
    public Optional<ObjectiveProgress> existingProgress(@NotNull ObjectiveSubject subject, @NotNull ContentId objectiveId) {
        Map<ContentId, ObjectiveProgress> byObjective = this.progress.get(requireNonNull(subject, "subject"));
        return byObjective == null ? Optional.empty() : Optional.ofNullable(byObjective.get(requireNonNull(objectiveId, "objectiveId")));
    }

    @NotNull
    public List<ObjectiveUpdate> apply(@NotNull ObjectiveEvent event) {
        requireNonNull(event, "event");
        List<ObjectiveUpdate> updates = new ArrayList<>();
        for (ObjectiveDefinition definition : this.definitions.values()) {
            ObjectiveProgress objectiveProgress = progress(event.subject(), definition.id());
            if (objectiveProgress.completed(definition)) {
                continue;
            }
            for (ObjectiveTask task : eligibleTasks(definition, objectiveProgress)) {
                if (!task.matches(event)) {
                    continue;
                }
                ObjectiveUpdate update = advance(definition, objectiveProgress, task, event);
                if (update != null) {
                    updates.add(update);
                    this.listeners.forEach(listener -> listener.progress(update));
                }
                if (definition.chained()) {
                    break;
                }
            }
        }
        return List.copyOf(updates);
    }

    public void reset(@NotNull ObjectiveSubject subject, @NotNull ContentId objectiveId) {
        existingProgress(subject, objectiveId).ifPresent(ObjectiveProgress::reset);
    }

    public void resetSubject(@NotNull ObjectiveSubject subject) {
        this.progress.remove(requireNonNull(subject, "subject"));
    }

    @NotNull
    private List<ObjectiveTask> eligibleTasks(@NotNull ObjectiveDefinition definition, @NotNull ObjectiveProgress progress) {
        if (!definition.chained()) {
            return definition.tasks().stream().filter(task -> !progress.completed(task)).toList();
        }
        return definition.tasks().stream()
                .filter(task -> !progress.completed(task))
                .findFirst()
                .map(List::of)
                .orElseGet(List::of);
    }

    private ObjectiveUpdate advance(
            @NotNull ObjectiveDefinition definition,
            @NotNull ObjectiveProgress progress,
            @NotNull ObjectiveTask task,
            @NotNull ObjectiveEvent event
    ) {
        long before = progress.current(task.id());
        boolean wasTaskComplete = progress.completed(task);
        long after = progress.advance(task, event.amount());
        if (after == before) {
            return null;
        }
        boolean taskCompleted = !wasTaskComplete && progress.completed(task);
        return new ObjectiveUpdate(definition, event.subject(), task, before, after, taskCompleted, progress.completed(definition), event);
    }
}
