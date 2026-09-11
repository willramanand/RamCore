package dev.willram.ramcore.item.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serializer-friendly representation of one data-component patch change.
 */
public record ItemComponentSerializedChange(
        @NotNull String component,
        @NotNull ItemComponentAction action,
        @Nullable String valueType,
        @Nullable Object value
) {
}
