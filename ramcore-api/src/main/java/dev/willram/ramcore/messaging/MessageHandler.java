package dev.willram.ramcore.messaging;

import org.jetbrains.annotations.NotNull;

/**
 * Receives a raw message from a {@link MessageBus} channel. Invoked on the async scheduler.
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * Handles one message.
     *
     * @param channel the channel it arrived on
     * @param payload the raw bytes
     */
    void handle(@NotNull String channel, byte @NotNull [] payload);
}
