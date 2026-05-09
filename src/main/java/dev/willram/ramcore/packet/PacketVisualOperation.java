package dev.willram.ramcore.packet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Logical packet operation sent to one viewer.
 */
public record PacketVisualOperation(
        @NotNull PacketVisualAction action,
        @Nullable Integer entityId,
        @NotNull Map<String, Object> data
) {

    public PacketVisualOperation {
        Objects.requireNonNull(action, "action");
        data = Map.copyOf(Objects.requireNonNull(data, "data"));
        if (entityId != null && entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
    }

    @NotNull
    public static PacketVisualOperation of(@NotNull PacketVisualAction action, @Nullable Integer entityId,
                                           @NotNull Map<String, Object> data) {
        return new PacketVisualOperation(action, entityId, data);
    }
}
