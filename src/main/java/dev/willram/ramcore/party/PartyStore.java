package dev.willram.ramcore.party;

import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.store.ForwardingStore;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.store.StoreCodec;
import dev.willram.ramcore.store.Stores;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence for parties. {@link PartyManager} writes through on every membership change and
 * deletes on disband; {@link PartyManager#load()} restores parties on startup.
 *
 * <p>Stability: stable. Folia-safe by design.</p>
 */
public interface PartyStore extends Store<PartyId, PartySnapshot> {

    /**
     * In-memory store: the default, so behaviour without persistence is unchanged.
     *
     * @return the store
     */
    @NotNull
    static PartyStore inMemory() {
        return of(Stores.inMemory());
    }

    /**
     * Adapts any backend, for example {@code Stores.file(dir, PartyStore.keyCodec(), PartyStore.codec())}.
     *
     * @param delegate the backend
     * @return the party store
     */
    @NotNull
    static PartyStore of(@NotNull Store<PartyId, PartySnapshot> delegate) {
        return new Forwarding(delegate);
    }

    /** Key codec for file and SQL backends. */
    @NotNull
    static DataKeyCodec<PartyId> keyCodec() {
        return new DataKeyCodec<>() {
            @Override
            public @NotNull String encode(@NotNull PartyId key) {
                return key.toString();
            }

            @Override
            public @NotNull PartyId decode(@NotNull String value) {
                return PartyId.of(value);
            }
        };
    }

    /** Value codec for file and SQL backends. */
    @NotNull
    static StoreCodec<PartySnapshot> codec() {
        return StoreCodec.gson(PartySnapshot.class);
    }

    final class Forwarding extends ForwardingStore<PartyId, PartySnapshot> implements PartyStore {
        private Forwarding(Store<PartyId, PartySnapshot> delegate) {
            super(delegate);
        }
    }
}
