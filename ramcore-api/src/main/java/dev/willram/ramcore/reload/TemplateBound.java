package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.scheduler.TaskContext;
import org.jetbrains.annotations.NotNull;

/**
 * A live object built from a content template that can be rebound when its template reloads. Register
 * instances in a {@link LiveObjectRegistry}; {@link ContentReloadService} rebuilds each on its
 * {@link #owner()} scheduler after a reload.
 */
public interface TemplateBound {

    /** The template this object was built from. */
    @NotNull
    ContentId templateId();

    /**
     * Applies the reloaded template. Called on {@link #owner()}'s thread. Should not throw; failures
     * are caught and logged by the reload service.
     *
     * @param resolved the object produced by the pack's resolver for the reloaded definition
     */
    void rebind(@NotNull Object resolved);

    /**
     * The scheduler that owns this object's state (entity for NPCs/displays, region for holograms,
     * global for regions). Defaults to {@link TaskContext#global()}.
     *
     * @return the owner scheduler context
     */
    @NotNull
    default TaskContext owner() {
        return TaskContext.global();
    }
}
