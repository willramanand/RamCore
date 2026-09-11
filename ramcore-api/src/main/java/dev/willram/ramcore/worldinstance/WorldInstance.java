package dev.willram.ramcore.worldinstance;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A running instanced world. {@link #close()} evacuates players, unloads the world, and (per options)
 * deletes its directory — so binding it to a {@code PartyGroup} or encounter via a terminable tears
 * the instance down with them.
 */
public final class WorldInstance implements Terminable {
    private final String name;
    private final String id;
    private final String template;
    private final Path directory;
    private final WorldBackend backend;
    private final WorldInstanceOptions options;
    private volatile boolean closed;

    WorldInstance(@NotNull String name, @NotNull String id, @NotNull String template, @NotNull Path directory,
                  @NotNull WorldBackend backend, @NotNull WorldInstanceOptions options) {
        this.name = requireNonNull(name, "name");
        this.id = requireNonNull(id, "id");
        this.template = requireNonNull(template, "template");
        this.directory = requireNonNull(directory, "directory");
        this.backend = requireNonNull(backend, "backend");
        this.options = requireNonNull(options, "options");
    }

    @NotNull
    public String name() {
        return this.name;
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String template() {
        return this.template;
    }

    @NotNull
    public Path directory() {
        return this.directory;
    }

    /**
     * Tears the instance down: evacuate → unload → (optionally) delete. Idempotent.
     *
     * @return a promise completing when teardown finishes
     */
    @NotNull
    public Promise<Void> closeAsync() {
        if (this.closed) {
            return Promise.completed(null);
        }
        this.closed = true;
        return this.backend.evacuate(this.name)
                .thenCompose(TaskContext.async(), ignored -> this.backend.unloadWorld(this.name, this.options.saveOnClose()))
                .thenRun(TaskContext.async(), () -> {
                    if (this.options.deleteOnClose()) {
                        try {
                            WorldInstances.deleteRecursively(this.directory);
                        } catch (Exception failure) {
                            RamLog.warn("failed to delete instance directory " + this.directory, failure);
                        }
                    }
                });
    }

    @Override
    public void close() {
        closeAsync();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
