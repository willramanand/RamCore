package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * What changed between the previous build and this one, plus the finished pack's SHA-1. Paths are the
 * in-pack file paths (e.g. {@code assets/myplugin/items/ruby.json}); {@code sha1Hex} is the hash of
 * the whole zip, for {@link ResourcePackMetadata} / {@link ResourcePackPrompt}.
 *
 * @param added     files present now but not before
 * @param changed   files whose content changed
 * @param removed   files present before but not now
 * @param unchanged files with identical content
 * @param sha1Hex   the finished zip's SHA-1 (40 hex chars)
 */
public record PackBuildReport(@NotNull List<String> added, @NotNull List<String> changed,
                              @NotNull List<String> removed, @NotNull List<String> unchanged,
                              @NotNull String sha1Hex) {

    public PackBuildReport {
        added = List.copyOf(added);
        changed = List.copyOf(changed);
        removed = List.copyOf(removed);
        unchanged = List.copyOf(unchanged);
        requireNonNull(sha1Hex, "sha1Hex");
    }

    /** Whether anything was added, changed, or removed since the previous build. */
    public boolean dirty() {
        return !this.added.isEmpty() || !this.changed.isEmpty() || !this.removed.isEmpty();
    }
}
