package dev.willram.ramcore.objective;

import dev.willram.ramcore.store.ForwardingStore;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.store.StoreCodec;
import dev.willram.ramcore.store.Stores;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence for {@link ObjectiveTracker} progress. The tracker writes a subject's progress on
 * every update and deletes it on reset; {@link ObjectiveTracker#load()} restores progress on startup.
 *
 * <p>Stability: stable. Folia-safe by design.</p>
 */
public interface ObjectiveProgressStore extends Store<ObjectiveProgressKey, ObjectiveProgressSnapshot> {

    /**
     * In-memory store: the default.
     *
     * @return the store
     */
    @NotNull
    static ObjectiveProgressStore inMemory() {
        return of(Stores.inMemory());
    }

    /**
     * Adapts any backend, for example
     * {@code Stores.file(dir, ObjectiveProgressKey.keyCodec(), ObjectiveProgressStore.codec())}.
     *
     * @param delegate the backend
     * @return the progress store
     */
    @NotNull
    static ObjectiveProgressStore of(@NotNull Store<ObjectiveProgressKey, ObjectiveProgressSnapshot> delegate) {
        return new Forwarding(delegate);
    }

    /** Value codec for file and SQL backends. */
    @NotNull
    static StoreCodec<ObjectiveProgressSnapshot> codec() {
        return StoreCodec.gson(ObjectiveProgressSnapshot.class);
    }

    final class Forwarding extends ForwardingStore<ObjectiveProgressKey, ObjectiveProgressSnapshot> implements ObjectiveProgressStore {
        private Forwarding(Store<ObjectiveProgressKey, ObjectiveProgressSnapshot> delegate) {
            super(delegate);
        }
    }
}
