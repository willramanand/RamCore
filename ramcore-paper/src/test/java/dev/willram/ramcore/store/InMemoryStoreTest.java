package dev.willram.ramcore.store;

import java.util.Optional;

public final class InMemoryStoreTest extends StoreContractTest {

    @Override
    protected Store<String, Profile> newStore(StoreMigrations<Profile> migrations) {
        return Stores.inMemory(migrations);
    }

    @Override
    protected void seed(Store<String, Profile> store, String key, int version, Profile value) {
        ((InMemoryStore<String, Profile>) store).seed(key, version, value);
    }

    @Override
    protected Optional<Integer> persistedVersion(Store<String, Profile> store, String key) {
        return ((InMemoryStore<String, Profile>) store).storedVersion(key);
    }
}
