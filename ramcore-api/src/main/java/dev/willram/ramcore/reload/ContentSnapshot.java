package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentDefinition;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentLoadResult;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A hash of every content definition at one point in time (id → node hash), used to diff one reload
 * against the previous state.
 */
public final class ContentSnapshot {
    private static final ContentSnapshot EMPTY = new ContentSnapshot(Map.of());

    private final Map<ContentId, String> hashes;

    private ContentSnapshot(@NotNull Map<ContentId, String> hashes) {
        this.hashes = Map.copyOf(hashes);
    }

    @NotNull
    public static ContentSnapshot empty() {
        return EMPTY;
    }

    /** A snapshot hashing every definition in a load result. */
    @NotNull
    public static ContentSnapshot of(@NotNull ContentLoadResult result) {
        requireNonNull(result, "result");
        Map<ContentId, String> hashes = new LinkedHashMap<>();
        for (ContentDefinition definition : result.definitions()) {
            hashes.put(definition.id(), ContentHashing.hash(definition.node()));
        }
        return new ContentSnapshot(hashes);
    }

    @NotNull
    public static ContentSnapshot of(@NotNull Map<ContentId, String> hashes) {
        return new ContentSnapshot(hashes);
    }

    @NotNull
    public Set<ContentId> ids() {
        return this.hashes.keySet();
    }

    @NotNull
    public Optional<String> hash(@NotNull ContentId id) {
        return Optional.ofNullable(this.hashes.get(id));
    }

    @NotNull
    public Map<ContentId, String> hashes() {
        return this.hashes;
    }

    public boolean isEmpty() {
        return this.hashes.isEmpty();
    }
}
