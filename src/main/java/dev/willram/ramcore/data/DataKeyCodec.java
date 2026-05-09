package dev.willram.ramcore.data;

import org.jetbrains.annotations.NotNull;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Encodes repository keys into file-safe names and decodes them back.
 */
public interface DataKeyCodec<K> {

    @NotNull
    String encode(@NotNull K key);

    @NotNull
    K decode(@NotNull String value);

    @NotNull
    static DataKeyCodec<String> stringKeys() {
        return new DataKeyCodec<>() {
            @Override
            public @NotNull String encode(@NotNull String key) {
                return URLEncoder.encode(Objects.requireNonNull(key, "key"), StandardCharsets.UTF_8);
            }

            @Override
            public @NotNull String decode(@NotNull String value) {
                return URLDecoder.decode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8);
            }
        };
    }

    @NotNull
    static DataKeyCodec<UUID> uuidKeys() {
        return new DataKeyCodec<>() {
            @Override
            public @NotNull String encode(@NotNull UUID key) {
                return Objects.requireNonNull(key, "key").toString();
            }

            @Override
            public @NotNull UUID decode(@NotNull String value) {
                return UUID.fromString(Objects.requireNonNull(value, "value"));
            }
        };
    }
}
