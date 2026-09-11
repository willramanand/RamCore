package dev.willram.ramcore.messaging;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.terminable.Terminable;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A minimal publish/subscribe bus for cross-plugin or cross-server messaging.
 *
 * <p>Handlers run on the async scheduler; callers that touch server state pick their own context.
 * Implementations: {@link InMemoryMessageBus} (single JVM, tests), {@code PluginMessagingBus}
 * (Bukkit plugin channels), {@code RedisMessageBus} (Lettuce, cross-server).</p>
 *
 * <p>Stability: experimental. Folia-safe: delivery is async and state is concurrent.</p>
 */
public interface MessageBus extends Terminable {

    /**
     * Publishes a raw message to a channel.
     *
     * @param channel the channel
     * @param payload the bytes
     * @return completes when the message has been dispatched
     */
    @NotNull
    Promise<Void> publish(@NotNull String channel, byte @NotNull [] payload);

    /**
     * Subscribes to a channel. Close the returned {@link Terminable} to unsubscribe.
     *
     * @param channel the channel
     * @param handler the handler
     * @return an unsubscribe handle
     */
    @NotNull
    Terminable subscribe(@NotNull String channel, @NotNull MessageHandler handler);

    /**
     * Publishes a typed message using a codec.
     *
     * @param channel the channel
     * @param message the message
     * @param codec   the codec
     * @param <T>     the message type
     * @return completes when dispatched
     */
    @NotNull
    default <T> Promise<Void> publish(@NotNull String channel, @NotNull T message, @NotNull MessageCodec<T> codec) {
        requireNonNull(codec, "codec");
        return publish(channel, codec.encode(requireNonNull(message, "message")));
    }

    /**
     * Subscribes to a channel with a codec, decoding each payload before handling.
     *
     * @param channel the channel
     * @param codec   the codec
     * @param handler the typed handler
     * @param <T>     the message type
     * @return an unsubscribe handle
     */
    @NotNull
    default <T> Terminable subscribe(@NotNull String channel, @NotNull MessageCodec<T> codec, @NotNull TypedMessageHandler<T> handler) {
        requireNonNull(codec, "codec");
        requireNonNull(handler, "handler");
        return subscribe(channel, (ch, payload) -> handler.handle(ch, codec.decode(payload)));
    }

    /**
     * A handler for decoded messages.
     *
     * @param <T> the message type
     */
    @FunctionalInterface
    interface TypedMessageHandler<T> {
        void handle(@NotNull String channel, @NotNull T message);
    }
}
