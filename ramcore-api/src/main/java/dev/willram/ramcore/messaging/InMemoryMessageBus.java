package dev.willram.ramcore.messaging;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.Terminable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * A single-JVM {@link MessageBus}: published messages are delivered to local subscribers on the
 * async scheduler. Useful for tests and single-server setups.
 */
public final class InMemoryMessageBus implements MessageBus {
    private final Map<String, List<MessageHandler>> handlers = new ConcurrentHashMap<>();
    private volatile boolean closed;

    @NotNull
    @Override
    public Promise<Void> publish(@NotNull String channel, byte @NotNull [] payload) {
        requireNonNull(channel, "channel");
        requireNonNull(payload, "payload");
        if (this.closed) {
            return Promise.completed(null);
        }
        List<MessageHandler> subscribers = this.handlers.get(channel);
        if (subscribers == null || subscribers.isEmpty()) {
            return Promise.completed(null);
        }
        byte[] copy = payload.clone();
        List<MessageHandler> snapshot = List.copyOf(subscribers);
        return Schedulers.runAsync(() -> snapshot.forEach(handler -> handler.handle(channel, copy.clone())));
    }

    @NotNull
    @Override
    public Terminable subscribe(@NotNull String channel, @NotNull MessageHandler handler) {
        requireNonNull(channel, "channel");
        requireNonNull(handler, "handler");
        List<MessageHandler> list = this.handlers.computeIfAbsent(channel, key -> new CopyOnWriteArrayList<>());
        list.add(handler);
        return () -> list.remove(handler);
    }

    @Override
    public void close() {
        this.closed = true;
        this.handlers.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
