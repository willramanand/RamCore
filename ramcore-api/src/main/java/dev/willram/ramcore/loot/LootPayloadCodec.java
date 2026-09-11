package dev.willram.ramcore.loot;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a {@link LootReward#payload()} into a string for persistence and back. RamCore never
 * interprets payloads, so a consumer that persists loot instances supplies the codec for whatever
 * it puts in them.
 */
public interface LootPayloadCodec {

    /**
     * Encodes a payload.
     *
     * @param payload the payload, may be null
     * @return the encoded form, or null for no payload
     */
    @Nullable
    String encode(@Nullable Object payload);

    /**
     * Decodes a payload.
     *
     * @param encoded the encoded form, or null for no payload
     * @return the payload, may be null
     */
    @Nullable
    Object decode(@Nullable String encoded);

    /**
     * Codec for string (or absent) payloads. Any other payload type fails at save time with an
     * actionable exception rather than being silently stringified.
     *
     * @return the codec
     */
    @NotNull
    static LootPayloadCodec strings() {
        return new LootPayloadCodec() {
            @Override
            public @Nullable String encode(@Nullable Object payload) {
                if (payload == null) {
                    return null;
                }
                RamPreconditions.checkArgument(payload instanceof String,
                        "loot reward payload of type " + payload.getClass().getName() + " cannot be persisted by the string codec",
                        "Pass a LootPayloadCodec that knows how to encode your payload type to InstancedLoot.persistentStore(..).");
                return (String) payload;
            }

            @Override
            public @Nullable Object decode(@Nullable String encoded) {
                return encoded;
            }
        };
    }
}
