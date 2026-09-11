package dev.willram.ramcore.worldinstance;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Creates instanced worlds by copying a template, marking the copy, and loading it, and sweeps
 * leftover instances on startup. Copy/marker/delete run on the async scheduler; the world load/unload
 * hop to the global region through the {@link WorldBackend}.
 *
 * <p>Runtime world creation is gated on {@link WorldBackend#supportsInstances()} — on a regionised
 * server (Folia) {@link #create} fails with an actionable message rather than corrupting state.
 * Stability: Paper-experimental.</p>
 */
public final class WorldInstanceService {
    private final WorldBackend backend;
    private final Path templatesRoot;

    /**
     * @param backend       the platform backend
     * @param templatesRoot the directory holding {@code <name>/} world templates
     */
    public WorldInstanceService(@NotNull WorldBackend backend, @NotNull Path templatesRoot) {
        this.backend = requireNonNull(backend, "backend");
        this.templatesRoot = requireNonNull(templatesRoot, "templatesRoot");
    }

    /**
     * Creates a world instance from a template.
     *
     * @param templateName the template directory name under the templates root
     * @param options      creation options
     * @return a promise of the running instance
     */
    @NotNull
    public Promise<WorldInstance> create(@NotNull String templateName, @NotNull WorldInstanceOptions options) {
        requireNonNull(templateName, "templateName");
        requireNonNull(options, "options");
        if (!this.backend.supportsInstances()) {
            return Promise.exceptionally(RamPreconditions.misuse(
                    "runtime world instances are not supported on this server",
                    "instanced worlds require a non-regionised server (Paper); Folia does not support runtime world creation"));
        }
        return Promise.<WorldInstance>supplyingExceptionally(TaskContext.async(), () -> {
            String id = UUID.randomUUID().toString().substring(0, 8);
            String name = WorldInstances.PREFIX + templateName + "_" + id;
            Path template = this.templatesRoot.resolve(templateName);
            Path directory = this.backend.worldContainer().resolve(name);
            WorldInstances.copyTemplate(template, directory);
            new InstanceMarker(templateName, id, System.currentTimeMillis()).writeTo(directory);
            return new WorldInstance(name, id, templateName, directory, this.backend, options);
        }).thenCompose(TaskContext.async(),
                instance -> this.backend.loadWorld(instance.name()).thenApply(TaskContext.async(), ignored -> instance));
    }

    /**
     * Deletes leftover instance directories from a previous run (identified by their marker). Call at
     * startup, before creating new instances. Blocking file I/O — run on the async scheduler.
     *
     * @return the number of directories deleted
     */
    public int sweepStartup() {
        int deleted = 0;
        try {
            List<Path> leftovers = WorldInstances.sweep(this.backend.worldContainer());
            for (Path directory : leftovers) {
                WorldInstances.deleteRecursively(directory);
                deleted++;
            }
        } catch (Exception failure) {
            RamLog.warn("world instance startup sweep failed", failure);
        }
        return deleted;
    }
}
