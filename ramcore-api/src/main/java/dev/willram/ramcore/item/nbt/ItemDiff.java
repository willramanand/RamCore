package dev.willram.ramcore.item.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured diff between two item snapshots.
 */
public final class ItemDiff {
    private final List<ItemDiffEntry> entries;

    private ItemDiff(List<ItemDiffEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    @NotNull
    public static ItemDiff between(@NotNull ItemSnapshot before, @NotNull ItemSnapshot after) {
        return between(before, after, EnumSet.noneOf(ItemDiffSection.class));
    }

    @NotNull
    public static ItemDiff between(@NotNull ItemSnapshot before, @NotNull ItemSnapshot after,
                                   @NotNull EnumSet<ItemDiffSection> ignored) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(ignored, "ignored");
        List<ItemDiffEntry> entries = new ArrayList<>();
        compare(entries, ignored, ItemDiffSection.TYPE, before.type(), after.type());
        compare(entries, ignored, ItemDiffSection.AMOUNT, before.amount(), after.amount());
        compare(entries, ignored, ItemDiffSection.META, before.meta(), after.meta());
        compare(entries, ignored, ItemDiffSection.PDC, before.pdc(), after.pdc());
        compare(entries, ignored, ItemDiffSection.DATA_COMPONENTS, before.components(), after.components());
        compare(entries, ignored, ItemDiffSection.ENCHANTMENTS, before.enchantments(), after.enchantments());
        compare(entries, ignored, ItemDiffSection.ATTRIBUTES, before.attributes(), after.attributes());
        compare(entries, ignored, ItemDiffSection.RAW_NBT, before.rawNbt(), after.rawNbt());
        return new ItemDiff(entries);
    }

    @NotNull
    public List<ItemDiffEntry> entries() {
        return this.entries;
    }

    public boolean empty() {
        return this.entries.isEmpty();
    }

    public boolean changed(@NotNull ItemDiffSection section) {
        return this.entries.stream().anyMatch(entry -> entry.section() == section);
    }

    @NotNull
    public Optional<ItemDiffEntry> entry(@NotNull ItemDiffSection section) {
        return this.entries.stream().filter(entry -> entry.section() == section).findFirst();
    }

    private static void compare(List<ItemDiffEntry> entries, EnumSet<ItemDiffSection> ignored,
                                ItemDiffSection section, Object before, Object after) {
        if (!ignored.contains(section) && !Objects.equals(before, after)) {
            entries.add(new ItemDiffEntry(section, before, after));
        }
    }
}
