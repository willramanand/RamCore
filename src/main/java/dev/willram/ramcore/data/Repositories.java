package dev.willram.ramcore.data;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Factory helpers for common repository implementations.
 */
public final class Repositories {

    @NotNull
    public static <V extends DataItem> FileDataRepository<String, V> jsonByString(
            @NotNull Path directory,
            @NotNull Class<V> type,
            @NotNull Executor saveExecutor
    ) {
        return new FileDataRepository<>(directory, DataKeyCodec.stringKeys(), GsonDataSerializer.pretty(type), saveExecutor);
    }

    @NotNull
    public static <V extends DataItem> FileDataRepository<UUID, V> jsonByUuid(
            @NotNull Path directory,
            @NotNull Class<V> type,
            @NotNull Executor saveExecutor
    ) {
        return new FileDataRepository<>(directory, DataKeyCodec.uuidKeys(), GsonDataSerializer.pretty(type), saveExecutor);
    }

    private Repositories() {
    }
}
