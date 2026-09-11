package dev.willram.ramcore.cooldown;

import dev.willram.ramcore.store.ForwardingStore;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.store.StoreCodec;
import dev.willram.ramcore.store.Stores;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence for a {@link CooldownTracker}. The tracker writes an entry when a cooldown is
 * consumed or reset and deletes it when removed or swept; {@link CooldownTracker#load()} restores
 * active cooldowns on startup.
 *
 * <p>Keys are the tracker's own key type, so file and SQL backends need a matching
 * {@code DataKeyCodec}; {@link CooldownKey} has one in {@link CooldownKey#keyCodec()}.</p>
 *
 * <p>Stability: stable. Folia-safe by design.</p>
 *
 * @param <K> key type
 */
public interface CooldownStore<K> extends Store<K, CooldownSnapshot> {

    /**
     * In-memory store: the default.
     *
     * @param <K> key type
     * @return the store
     */
    @NotNull
    static <K> CooldownStore<K> inMemory() {
        return of(Stores.inMemory());
    }

    /**
     * Adapts any backend.
     *
     * @param delegate the backend
     * @param <K>      key type
     * @return the cooldown store
     */
    @NotNull
    static <K> CooldownStore<K> of(@NotNull Store<K, CooldownSnapshot> delegate) {
        return new Forwarding<>(delegate);
    }

    /** Value codec for file and SQL backends. */
    @NotNull
    static StoreCodec<CooldownSnapshot> codec() {
        return StoreCodec.gson(CooldownSnapshot.class);
    }

    final class Forwarding<K> extends ForwardingStore<K, CooldownSnapshot> implements CooldownStore<K> {
        private Forwarding(Store<K, CooldownSnapshot> delegate) {
            super(delegate);
        }
    }
}
