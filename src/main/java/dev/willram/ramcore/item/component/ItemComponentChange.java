package dev.willram.ramcore.item.component;

import io.papermc.paper.datacomponent.DataComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One operation in an item data-component patch.
 */
public record ItemComponentChange(
        @NotNull DataComponentType type,
        @NotNull ItemComponentAction action,
        @Nullable Object value
) {
    public ItemComponentChange {
        type = Objects.requireNonNull(type, "type");
        action = Objects.requireNonNull(action, "action");
    }
}
