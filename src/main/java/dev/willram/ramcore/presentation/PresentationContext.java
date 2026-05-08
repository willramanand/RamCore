package dev.willram.ramcore.presentation;

import dev.willram.ramcore.scheduler.TaskContext;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Target audiences and scheduler anchor for presentation effects.
 */
public final class PresentationContext {
    private final List<Audience> audiences;
    private final TaskContext taskContext;

    private PresentationContext(@NotNull Collection<? extends Audience> audiences, @NotNull TaskContext taskContext) {
        this.audiences = List.copyOf(requireNonNull(audiences, "audiences"));
        this.taskContext = requireNonNull(taskContext, "taskContext");
    }

    @NotNull
    public static PresentationContext of(@NotNull Audience audience) {
        return of(TaskContext.global(), List.of(audience));
    }

    @NotNull
    public static PresentationContext of(@NotNull Audience... audiences) {
        return of(TaskContext.global(), List.of(audiences));
    }

    @NotNull
    public static PresentationContext of(@NotNull TaskContext taskContext, @NotNull Audience... audiences) {
        return of(taskContext, List.of(audiences));
    }

    @NotNull
    public static PresentationContext of(@NotNull TaskContext taskContext, @NotNull Collection<? extends Audience> audiences) {
        return new PresentationContext(audiences, taskContext);
    }

    @NotNull
    public List<Audience> audiences() {
        return this.audiences;
    }

    @NotNull
    public TaskContext taskContext() {
        return this.taskContext;
    }

    public boolean empty() {
        return this.audiences.isEmpty();
    }

    public void forEachAudience(@NotNull Consumer<Audience> consumer) {
        requireNonNull(consumer, "consumer");
        for (Audience audience : this.audiences) {
            consumer.accept(audience);
        }
    }
}
