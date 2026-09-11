package dev.willram.ramcore.item.component;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Serializer-friendly component patch payload with an explicit format version.
 */
public record ItemComponentSerializedPatch(
        int formatVersion,
        @NotNull List<ItemComponentSerializedChange> changes
) {
    public static final int CURRENT_FORMAT = 1;

    public ItemComponentSerializedPatch {
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
    }

    @NotNull
    public static ItemComponentSerializedPatch of(@NotNull ItemComponentPatch patch) {
        return new ItemComponentSerializedPatch(CURRENT_FORMAT, patch.changes().stream()
                .map(change -> new ItemComponentSerializedChange(
                        change.type().getKey().toString(),
                        change.action(),
                        change.value() == null ? null : change.value().getClass().getName(),
                        change.value()
                ))
                .toList());
    }
}
