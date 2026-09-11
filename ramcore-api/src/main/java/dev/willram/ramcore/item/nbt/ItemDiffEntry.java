package dev.willram.ramcore.item.nbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One changed section in an item diff.
 */
public record ItemDiffEntry(
        @NotNull ItemDiffSection section,
        @Nullable Object before,
        @Nullable Object after
) {
    public ItemDiffEntry {
        section = Objects.requireNonNull(section, "section");
    }
}
