package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.store.CachedStore;
import dev.willram.ramcore.store.StoreCodec;
import dev.willram.ramcore.store.StoreContractTest.Profile;
import dev.willram.ramcore.store.Stores;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pooled path: HikariCP is on the test classpath, so {@code Stores.sql(config, ..)} works.
 */
public final class SqlStoreHikariTest {
    @TempDir
    Path tempDir;

    private FakeScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
    }

    @Test
    public void pooledSqliteStoreRoundTripsAndClosesThePool() {
        SqlStoreConfig config = SqlStoreConfig.sqlite(this.tempDir.resolve("pooled.db"));
        SqlStore<UUID, Profile> store = Stores.sql(config, "profiles", DataKeyCodec.uuidKeys(), StoreCodec.gson(Profile.class));
        UUID id = UUID.randomUUID();

        Promise<Void> saved = store.save(id, new Profile("will", 3));
        Promise<Optional<Profile>> loaded = store.load(id);
        this.scheduler.runAll();

        assertTrue(saved.isDone());
        assertEquals(3, loaded.join().orElseThrow().level());
        assertFalse(store.isClosed());

        store.close();
        assertTrue(store.isClosed());
    }

    @Test
    public void cachedStoreOverSqlFlushesOnClose() {
        SqlStoreConfig config = SqlStoreConfig.sqlite(this.tempDir.resolve("cached.db"));
        CachedStore<String, Profile> cache = Stores.cached(Stores.sql(config, "profiles", DataKeyCodec.stringKeys(), StoreCodec.gson(Profile.class)));
        cache.put("will", new Profile("will", 5));

        // drain the async queue before close: close() joins the flush, and the fake scheduler only
        // runs when driven, so flush first, then close
        cache.flush();
        this.scheduler.runAll();
        cache.close();

        SqlStore<String, Profile> fresh = Stores.sql(config, "profiles", DataKeyCodec.stringKeys(), StoreCodec.gson(Profile.class));
        Promise<Optional<Profile>> loaded = fresh.load("will");
        this.scheduler.runAll();
        assertEquals(5, loaded.join().orElseThrow().level());
        fresh.close();
    }

    @Test
    public void hikariAvailabilityIsDetected() {
        assertTrue(HikariConnectionProviders.available());
    }
}
