package dev.willram.ramcore.messaging;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.Terminable;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * A cross-server {@link MessageBus} over Redis pub/sub (Lettuce). Lettuce is resolved at runtime by
 * the plugin loader (ADR-0003), so this class is only usable when {@code storage}/{@code messaging}
 * Redis support is enabled and the driver is present.
 *
 * <p>Handlers run on the async scheduler. Channels are UTF-8 encoded; payloads are raw bytes.
 * Stability: experimental (not exercised without a live Redis).</p>
 */
public final class RedisMessageBus implements MessageBus {
    private final RedisClient client;
    private final StatefulRedisPubSubConnection<byte[], byte[]> subscribeConnection;
    private final StatefulRedisPubSubConnection<byte[], byte[]> publishConnection;
    private final Map<String, List<MessageHandler>> handlers = new ConcurrentHashMap<>();
    private volatile boolean closed;

    private RedisMessageBus(RedisClient client) {
        this.client = client;
        this.subscribeConnection = client.connectPubSub(ByteArrayCodec.INSTANCE);
        this.publishConnection = client.connectPubSub(ByteArrayCodec.INSTANCE);
        this.subscribeConnection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(byte[] channel, byte[] message) {
                deliver(new String(channel, StandardCharsets.UTF_8), message);
            }
        });
    }

    /**
     * Connects to Redis at the given URI (for example {@code redis://localhost:6379}).
     *
     * @param uri the Redis URI
     * @return the bus
     */
    @NotNull
    public static RedisMessageBus connect(@NotNull String uri) {
        requireNonNull(uri, "uri");
        return new RedisMessageBus(RedisClient.create(uri));
    }

    @NotNull
    @Override
    public Promise<Void> publish(@NotNull String channel, byte @NotNull [] payload) {
        requireNonNull(channel, "channel");
        requireNonNull(payload, "payload");
        return Promise.wrapFuture(
                this.publishConnection.async()
                        .publish(channel.getBytes(StandardCharsets.UTF_8), payload)
                        .toCompletableFuture()
                        .thenApply(count -> (Void) null)
        );
    }

    @NotNull
    @Override
    public Terminable subscribe(@NotNull String channel, @NotNull MessageHandler handler) {
        requireNonNull(channel, "channel");
        requireNonNull(handler, "handler");
        List<MessageHandler> list = this.handlers.computeIfAbsent(channel, key -> {
            this.subscribeConnection.sync().subscribe(key.getBytes(StandardCharsets.UTF_8));
            return new CopyOnWriteArrayList<>();
        });
        list.add(handler);
        return () -> list.remove(handler);
    }

    private void deliver(String channel, byte[] payload) {
        List<MessageHandler> subscribers = this.handlers.get(channel);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        List<MessageHandler> snapshot = List.copyOf(subscribers);
        Schedulers.runAsync(() -> snapshot.forEach(handler -> handler.handle(channel, payload)));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.handlers.clear();
        this.subscribeConnection.close();
        this.publishConnection.close();
        this.client.shutdown();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
