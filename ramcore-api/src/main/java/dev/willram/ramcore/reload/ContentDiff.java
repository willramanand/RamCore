package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentDefinition;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentLoadResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * What a reload changed: data-level {@code added}/{@code removed}/{@code changed} ids, definitions
 * whose {@code extends} parent no longer resolves ({@code brokenReferences}), the ids whose live
 * objects were rebuilt, the ids whose resolution failed, and any load errors.
 *
 * @param added            ids present now but not before
 * @param removed          ids present before but not now
 * @param changed          ids whose node hash changed
 * @param brokenReferences ids whose {@code extends} parent is missing
 * @param rebuilt          ids whose live objects were rebuilt
 * @param failed           ids whose resolution threw
 * @param errors           load error messages
 */
public record ContentDiff(@NotNull List<ContentId> added, @NotNull List<ContentId> removed,
                          @NotNull List<ContentId> changed, @NotNull List<ContentId> brokenReferences,
                          @NotNull List<ContentId> rebuilt, @NotNull List<ContentId> failed,
                          @NotNull List<String> errors) {

    public ContentDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        changed = List.copyOf(changed);
        brokenReferences = List.copyOf(brokenReferences);
        rebuilt = List.copyOf(rebuilt);
        failed = List.copyOf(failed);
        errors = List.copyOf(errors);
    }

    /**
     * The data-level diff between two snapshots plus broken references and load errors from the new
     * load result. {@code rebuilt}/{@code failed} start empty; the reload service fills them.
     *
     * @param previous the previous snapshot
     * @param next     the new snapshot
     * @param result   the new load result (for broken refs + errors)
     * @return the diff
     */
    @NotNull
    public static ContentDiff data(@NotNull ContentSnapshot previous, @NotNull ContentSnapshot next,
                                   @NotNull ContentLoadResult result) {
        requireNonNull(previous, "previous");
        requireNonNull(next, "next");
        requireNonNull(result, "result");

        List<ContentId> added = new ArrayList<>();
        List<ContentId> changed = new ArrayList<>();
        List<ContentId> removed = new ArrayList<>();
        next.hashes().forEach((id, hash) -> {
            String old = previous.hash(id).orElse(null);
            if (old == null) {
                added.add(id);
            } else if (!old.equals(hash)) {
                changed.add(id);
            }
        });
        previous.ids().forEach(id -> {
            if (!next.ids().contains(id)) {
                removed.add(id);
            }
        });

        List<ContentId> broken = new ArrayList<>();
        for (ContentDefinition definition : result.definitions()) {
            if (definition.parent() != null && !next.ids().contains(definition.parent())) {
                broken.add(definition.id());
            }
        }

        List<String> errors = new ArrayList<>();
        result.errors().forEach(error -> errors.add(error.toString()));

        return new ContentDiff(added, removed, changed, broken, List.of(), List.of(), errors);
    }

    /**
     * A copy of this diff with the rebuild results filled in.
     *
     * @param rebuilt ids whose live objects were rebuilt
     * @param failed  ids whose resolution failed
     * @return the diff
     */
    @NotNull
    public ContentDiff withRebuild(@NotNull List<ContentId> rebuilt, @NotNull List<ContentId> failed) {
        return new ContentDiff(this.added, this.removed, this.changed, this.brokenReferences, rebuilt, failed,
                this.errors);
    }

    /** Whether anything changed at the data level. */
    public boolean dirty() {
        return !this.added.isEmpty() || !this.removed.isEmpty() || !this.changed.isEmpty();
    }
}
