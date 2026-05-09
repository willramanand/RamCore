package dev.willram.ramcore.entity;

import dev.willram.ramcore.terminable.Terminable;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Temporary entity mutation scope that restores captured state on close.
 */
public final class EntityTemporaryModifier<T extends Entity> implements Terminable {
    private final T entity;
    private final EntityControlSnapshot snapshot;
    private final AtomicBoolean closed = new AtomicBoolean();

    EntityTemporaryModifier(@NotNull T entity) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = EntityControlSnapshot.capture(entity);
    }

    @NotNull
    public EntityControl<T> control() {
        return EntityControls.control(this.entity);
    }

    @NotNull
    public T entity() {
        return this.entity;
    }

    @Override
    public void close() {
        if (this.closed.getAndSet(true)) {
            return;
        }
        this.snapshot.restore(this.entity);
    }

    @Override
    public boolean isClosed() {
        return this.closed.get();
    }
}
