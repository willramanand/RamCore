package dev.willram.ramcore.event;

import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.terminable.TerminableConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owns a group of event subscriptions and unregisters them together.
 */
public final class EventSubscriptionGroup implements TerminableConsumer, Terminable {
    private final List<AutoCloseable> subscriptions = new ArrayList<>();
    private boolean closed;

    @NotNull
    public static EventSubscriptionGroup create() {
        return new EventSubscriptionGroup();
    }

    @Override
    public synchronized @NotNull <T extends AutoCloseable> T bind(@NotNull T terminable) {
        Objects.requireNonNull(terminable, "terminable");
        if (this.closed) {
            close(terminable);
            return terminable;
        }
        this.subscriptions.add(terminable);
        return terminable;
    }

    @NotNull
    public synchronized List<AutoCloseable> subscriptions() {
        return List.copyOf(this.subscriptions);
    }

    public synchronized int size() {
        return this.subscriptions.size();
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        RuntimeException first = null;
        for (int i = this.subscriptions.size() - 1; i >= 0; i--) {
            try {
                this.subscriptions.get(i).close();
            } catch (Exception e) {
                if (first == null) {
                    first = new IllegalStateException("event subscription cleanup failed", e);
                } else {
                    first.addSuppressed(e);
                }
            }
        }
        this.subscriptions.clear();
        if (first != null) {
            throw first;
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return this.closed;
    }

    private static void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new IllegalStateException("event subscription cleanup failed", e);
        }
    }
}
