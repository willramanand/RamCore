package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentDefinition;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentLoadResult;
import dev.willram.ramcore.content.ContentLoader;
import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Reloads registered {@link ContentPack}s: reruns {@code ContentLoader} off-thread, diffs against the
 * pack's previous {@link ContentSnapshot}, resolves the added/changed definitions, and rebuilds each
 * affected {@link TemplateBound} on its owner scheduler. Failures are collected, never thrown.
 *
 * <p>Threading: the load, diff, and resolution run on the async scheduler; each rebind is dispatched
 * to its object's {@link TemplateBound#owner()} scheduler (so a rebind may complete after the returned
 * {@link ContentDiff}). The diff's {@code failed} lists resolution failures; a rebind that throws on
 * its owner thread is caught and logged. Stability: experimental.</p>
 */
public final class ContentReloadService {
    private final LiveObjectRegistry liveObjects;
    private final Map<String, ContentPack> packs = new ConcurrentHashMap<>();
    private final Map<String, ContentSnapshot> snapshots = new ConcurrentHashMap<>();

    public ContentReloadService() {
        this(new LiveObjectRegistry());
    }

    public ContentReloadService(@NotNull LiveObjectRegistry liveObjects) {
        this.liveObjects = requireNonNull(liveObjects, "liveObjects");
    }

    @NotNull
    public LiveObjectRegistry liveObjects() {
        return this.liveObjects;
    }

    /** Registers a reloadable pack under its name. */
    public void register(@NotNull ContentPack pack) {
        requireNonNull(pack, "pack");
        this.packs.put(pack.name(), pack);
    }

    @NotNull
    public Optional<ContentPack> pack(@NotNull String name) {
        return Optional.ofNullable(this.packs.get(name));
    }

    /**
     * Reloads a registered pack by name.
     *
     * @param name the pack name
     * @return a promise of the diff
     */
    @NotNull
    public Promise<ContentDiff> reload(@NotNull String name) {
        ContentPack pack = this.packs.get(requireNonNull(name, "name"));
        RamPreconditions.checkArgument(pack != null, "no content pack named '" + name + "'",
                "register the pack before reloading it");
        return reload(pack);
    }

    /**
     * Reloads a pack.
     *
     * @param pack the pack
     * @return a promise of the diff
     */
    @NotNull
    public Promise<ContentDiff> reload(@NotNull ContentPack pack) {
        requireNonNull(pack, "pack");
        return Promise.supplyingAsync(() -> doReload(pack));
    }

    private ContentDiff doReload(@NotNull ContentPack pack) {
        ContentLoadResult result = ContentLoader.load(pack.root());
        ContentSnapshot next = ContentSnapshot.of(result);
        ContentSnapshot previous = this.snapshots.getOrDefault(pack.name(), ContentSnapshot.empty());
        ContentDiff diff = ContentDiff.data(previous, next, result);

        List<ContentId> affected = new ArrayList<>(diff.added());
        affected.addAll(diff.changed());

        List<ContentId> rebuilt = new ArrayList<>();
        List<ContentId> failed = new ArrayList<>();
        for (ContentId id : affected) {
            List<TemplateBound> objects = this.liveObjects.boundTo(id);
            if (objects.isEmpty()) {
                continue;
            }
            Object resolved;
            try {
                ContentDefinition definition = result.definition(id).orElseThrow();
                resolved = pack.resolver().resolve(definition);
            } catch (Exception failure) {
                failed.add(id);
                RamLog.warn("content reload: resolve failed for " + id, failure);
                continue;
            }
            for (TemplateBound object : objects) {
                Schedulers.run(object.owner(), () -> rebindSafely(object, resolved, id));
            }
            rebuilt.add(id);
        }

        this.snapshots.put(pack.name(), next);
        return diff.withRebuild(rebuilt, failed);
    }

    private static void rebindSafely(@NotNull TemplateBound object, @NotNull Object resolved, @NotNull ContentId id) {
        try {
            object.rebind(resolved);
        } catch (RuntimeException failure) {
            RamLog.warn("content reload: rebind failed for " + id, failure);
        }
    }
}
