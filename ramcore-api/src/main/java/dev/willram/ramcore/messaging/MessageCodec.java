package dev.willram.ramcore.messaging;

import dev.willram.ramcore.gson.GsonProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

/**
 * Encodes and decodes a message payload.
 *
 * @param <T> the message type
 */
public interface MessageCodec<T> {

    byte @NotNull [] encode(@NotNull T message);

    @NotNull
    T decode(byte @NotNull [] payload);

    /**
     * A JSON codec using RamCore's standard Gson.
     *
     * @param type the message type
     * @param <T>  the message type
     * @return the codec
     */
    @NotNull
    static <T> MessageCodec<T> gson(@NotNull Class<T> type) {
        requireNonNull(type, "type");
        return new MessageCodec<>() {
            @Override
            public byte @NotNull [] encode(@NotNull T message) {
                return GsonProvider.standard().toJson(message).getBytes(StandardCharsets.UTF_8);
            }

            @NotNull
            @Override
            public T decode(byte @NotNull [] payload) {
                return GsonProvider.standard().fromJson(new String(payload, StandardCharsets.UTF_8), type);
            }
        };
    }
}
