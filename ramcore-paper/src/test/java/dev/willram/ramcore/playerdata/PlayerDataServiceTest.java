package dev.willram.ramcore.playerdata;

import dev.willram.ramcore.exception.ApiMisuseException;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.store.InMemoryStore;
import dev.willram.ramcore.store.Stores;
import dev.willram.ramcore.testkit.FakeClock;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import dev.willram.ramcore.testkit.TestServiceContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PlayerDataServiceTest {
    private static final PlayerDataKey<Profile> PROFILE = PlayerDataKey.of("profile", Profile.class, () -> new Profile("new", 0));
    private static final PlayerDataKey<Stats> STATS = PlayerDataKey.of("stats", Stats.class, Stats::new, Stats::copy);
    private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(1); // 20 ticks

    private FakeScheduler scheduler;
    private FakeClock clock;
    private AsyncMapStore<UUID, Profile> profiles;
    private AsyncMapStore<UUID, Stats> stats;
    private UUID id;
    private ProxyFakes.Recording<Player> player;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.clock = FakeClock.epoch();
        this.profiles = new AsyncMapStore<>();
        this.stats = new AsyncMapStore<>();
        this.id = UUID.randomUUID();
        this.player = ProxyFakes.recording(Player.class, Map.of("getUniqueId", this.id, "getName", "will"));
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
    }

    private PlayerDataService service(JoinPolicy policy) {
        PlayerDataOptions options = PlayerDataOptions.defaults()
                .withJoinPolicy(policy)
                .withLoadTimeout(LOAD_TIMEOUT)
                .withAutosaveInterval(Duration.ofSeconds(5)); // 100 ticks
        PlayerDataService service = PlayerDataService.create(options, this.clock);
        service.register(PROFILE, this.profiles);
        service.register(STATS, this.stats);
        return service;
    }

    private void loadAndJoin(PlayerDataService service) {
        service.preload(this.id);
        this.scheduler.runAll();
        service.join(this.player.fake());
        assertTrue(service.isLoaded(this.id));
    }

    // ---- happy path ----

    @Test
    public void preloadThenJoinPromotesAndReadsSynchronously() {
        this.profiles.entries.put(this.id, new Profile("will", 7));
        PlayerDataService service = service(JoinPolicy.KICK);

        Promise<Void> load = service.preload(this.id);
        assertFalse(load.isDone());
        assertTrue(service.isPending(this.id));
        assertFalse(service.isLoaded(this.id));

        this.scheduler.runAll();
        assertTrue(load.isDone());
        assertFalse(service.isLoaded(this.id), "loaded only after join");
        assertTrue(service.get(this.id, PROFILE).isEmpty());

        service.join(this.player.fake());
        assertTrue(service.isLoaded(this.id));
        assertEquals(new Profile("will", 7), service.require(this.id, PROFILE));
        assertEquals(0, service.require(this.id, STATS).kills, "absent value gets the default");
        assertTrue(service.isDirty(this.id, STATS), "a fresh default is dirty so the first save persists it");
        assertFalse(service.isDirty(this.id, PROFILE));
        assertTrue(service.whenReady(this.id).isDone());
        assertEquals(java.util.Set.of(this.id), service.loadedPlayers());
    }

    @Test
    public void preloadIsIdempotentWhilePending() {
        PlayerDataService service = service(JoinPolicy.KICK);
        service.preload(this.id);
        service.preload(this.id);
        this.scheduler.runAll();
        assertEquals(1, this.profiles.calls.stream().filter(c -> c.startsWith("load:")).count());
    }

    @Test
    public void joinWithoutPreloadLoadsNow() {
        PlayerDataService service = service(JoinPolicy.DEFER);
        service.join(this.player.fake());
        assertTrue(service.isPending(this.id));
        this.scheduler.runAll();
        assertTrue(service.isLoaded(this.id));
    }

    // ---- join before load ----

    @Test
    public void joinBeforeLoadWithKickKicksAfterTimeout() {
        PlayerDataService service = service(JoinPolicy.KICK);
        service.preload(this.id);
        service.join(this.player.fake());

        assertFalse(service.isLoaded(this.id));
        this.scheduler.tick(19);
        assertFalse(this.player.called("kick"));
        // the load has still not completed: the async queue was never drained. The timeout fires
        // at tick 20 and the kick itself runs on the player's scheduler one tick later.
        this.scheduler.tick(2);
        assertTrue(this.player.called("kick"), "kicked once loadTimeout elapsed without data");
    }

    @Test
    public void joinBeforeLoadWithKickPromotesWhenLoadLandsInTime() {
        PlayerDataService service = service(JoinPolicy.KICK);
        service.preload(this.id);
        service.join(this.player.fake());

        this.scheduler.runAsync();
        assertTrue(service.isLoaded(this.id), "promoted as soon as the load completes");
        this.scheduler.tick(40);
        assertFalse(this.player.called("kick"));
    }

    @Test
    public void joinBeforeLoadWithDeferReturnsEmptyUntilReady() {
        PlayerDataService service = service(JoinPolicy.DEFER);
        service.preload(this.id);
        service.join(this.player.fake());

        Promise<Void> ready = service.whenReady(this.id);
        assertFalse(ready.isDone());
        assertTrue(service.get(this.id, PROFILE).isEmpty());
        assertThrows(ApiMisuseException.class, () -> service.require(this.id, PROFILE));

        this.scheduler.tick(40);
        assertFalse(this.player.called("kick"), "DEFER never kicks");

        this.scheduler.runAll();
        assertTrue(ready.isDone());
        assertTrue(service.get(this.id, PROFILE).isPresent());
    }

    @Test
    public void failedLoadUnderKickKicksImmediately() {
        this.profiles.failLoadsWith = new IllegalStateException("disk on fire");
        PlayerDataService service = service(JoinPolicy.KICK);
        service.preload(this.id);
        service.join(this.player.fake());

        this.scheduler.runAll();
        this.scheduler.tick(); // the kick runs on the player's scheduler
        assertTrue(this.player.called("kick"));
        assertFalse(service.isLoaded(this.id));
        assertFalse(service.isPending(this.id));
    }

    @Test
    public void failedLoadUnderDeferFailsReadiness() {
        this.profiles.failLoadsWith = new IllegalStateException("disk on fire");
        PlayerDataService service = service(JoinPolicy.DEFER);
        service.preload(this.id);
        service.join(this.player.fake());
        Promise<Void> ready = service.whenReady(this.id);

        this.scheduler.runAll();
        assertFalse(this.player.called("kick"));
        assertThrows(CompletionException.class, ready::join);
        assertTrue(service.get(this.id, PROFILE).isEmpty());
    }

    // ---- cancelled login ----

    @Test
    public void cancelledLoginEvictsPending() {
        PlayerDataService service = service(JoinPolicy.KICK);
        Promise<Void> ready = service.whenReady(this.id);
        assertThrows(CompletionException.class, ready::join, "unknown player fails readiness");

        service.preload(this.id);
        ready = service.whenReady(this.id);
        assertTrue(service.cancelPending(this.id));
        assertFalse(service.cancelPending(this.id));

        this.scheduler.runAll();
        assertFalse(service.isPending(this.id));
        assertFalse(service.isLoaded(this.id));
        assertThrows(CompletionException.class, ready::join);
        assertEquals(0, this.profiles.saves(), "a cancelled login never writes the default");
    }

    @Test
    public void cancelPendingDoesNotTouchLoadedPlayers() {
        PlayerDataService service = service(JoinPolicy.KICK);
        loadAndJoin(service);
        assertFalse(service.cancelPending(this.id));
        assertTrue(service.isLoaded(this.id));
    }

    @Test
    public void sweepEvictsStalePendingEntriesOnly() {
        PlayerDataService service = service(JoinPolicy.KICK);
        UUID stale = UUID.randomUUID();
        service.preload(stale);
        this.clock.advance(LOAD_TIMEOUT.multipliedBy(5));
        service.preload(this.id);

        assertEquals(1, service.sweepPending());
        assertFalse(service.isPending(stale));
        assertTrue(service.isPending(this.id), "young entry survives");

        this.scheduler.runAll();
        service.join(this.player.fake());
        this.clock.advance(LOAD_TIMEOUT.multipliedBy(5));
        assertEquals(0, service.sweepPending(), "joined players are never swept");
        assertTrue(service.isLoaded(this.id));
    }

    // ---- writes ----

    @Test
    public void setAndMarkDirtyRequireALoadedPlayer() {
        PlayerDataService service = service(JoinPolicy.KICK);
        assertThrows(ApiMisuseException.class, () -> service.set(this.id, PROFILE, new Profile("x", 1)));
        assertThrows(ApiMisuseException.class, () -> service.markDirty(this.id, PROFILE));

        loadAndJoin(service);
        service.set(this.id, PROFILE, new Profile("will", 8));
        assertTrue(service.isDirty(this.id, PROFILE));
        assertEquals(8, service.require(this.id, PROFILE).level());
    }

    @Test
    public void quitSavesDirtyKeysInlineAndEvicts() {
        this.profiles.entries.put(this.id, new Profile("will", 1));
        PlayerDataService service = service(JoinPolicy.KICK);
        loadAndJoin(service);
        service.set(this.id, PROFILE, new Profile("will", 2));
        service.require(this.id, STATS).kills = 3;
        service.markDirty(this.id, STATS);

        Promise<Void> quit = service.quit(this.id);
        assertFalse(service.isLoaded(this.id));
        assertEquals(0, this.scheduler.pendingSync(), "quit copies inline, no hop to the player thread");

        this.scheduler.runAll();
        assertTrue(quit.isDone());
        assertEquals(new Profile("will", 2), this.profiles.entries.get(this.id));
        assertEquals(3, this.stats.entries.get(this.id).kills);
    }

    @Test
    public void quitBeforeLoadSavesNothing() {
        PlayerDataService service = service(JoinPolicy.DEFER);
        service.preload(this.id);
        service.join(this.player.fake());
        service.quit(this.id);
        this.scheduler.runAll();
        assertEquals(0, this.profiles.saves());
        assertEquals(0, this.stats.saves());
        assertFalse(service.isPending(this.id));
    }

    @Test
    public void saveHopsToThePlayerThreadThenWritesAsync() {
        this.profiles.entries.put(this.id, new Profile("will", 1));
        this.stats.entries.put(this.id, new Stats());
        PlayerDataService service = service(JoinPolicy.KICK);
        loadAndJoin(service);
        service.set(this.id, PROFILE, new Profile("will", 5));

        Promise<Void> save = service.save(this.id);
        assertFalse(service.isDirty(this.id, PROFILE), "dirty mark cleared when the save is taken");
        assertEquals(1, this.scheduler.pendingScheduled(), "snapshot scheduled on the player's scheduler");
        assertEquals(0, this.scheduler.pendingAsync(), "no write until the snapshot is taken");
        assertEquals(0, this.profiles.saves());

        this.scheduler.tick();
        assertEquals(1, this.scheduler.pendingAsync(), "snapshot taken, write queued async");
        this.scheduler.runAll();
        assertTrue(save.isDone());
        assertEquals(new Profile("will", 5), this.profiles.entries.get(this.id));
        assertEquals(0, this.stats.saves(), "clean keys are not written");
    }

    @Test
    public void snapshotIsTakenOnThePlayerThreadSoLaterMutationsAreNotWritten() {
        this.profiles.entries.put(this.id, new Profile("will", 1));
        this.stats.entries.put(this.id, new Stats());
        PlayerDataService service = service(JoinPolicy.KICK);
        loadAndJoin(service);
        Stats live = service.require(this.id, STATS);
        live.kills = 10;
        service.markDirty(this.id, STATS);

        service.save(this.id);
        this.scheduler.tick(); // runs the snapshot on the player's thread; write is now queued async
        live.kills = 11;       // mutation after the copy, before the async write
        this.scheduler.runAsync();

        assertEquals(10, this.stats.entries.get(this.id).kills);
    }

    @Test
    public void autosaveOnlyTouchesDirtyKeys() {
        this.profiles.entries.put(this.id, new Profile("will", 1));
        this.stats.entries.put(this.id, new Stats());
        PlayerDataService service = service(JoinPolicy.KICK);
        TestServiceContext context = new TestServiceContext();
        service.enable(context);
        loadAndJoin(service);
        service.markDirty(this.id, STATS);

        this.scheduler.tick(99);
        assertEquals(0, this.stats.saves());
        this.scheduler.tick(1);  // autosave timer is due: queued on the async lane
        this.scheduler.runAll(); // timer runs and schedules the snapshot on the player
        this.scheduler.tick(1);  // snapshot taken, write queued async
        this.scheduler.runAll();
        assertEquals(1, this.stats.saves());
        assertEquals(0, this.profiles.saves());

        this.scheduler.tick(100);
        this.scheduler.runAll();
        assertEquals(1, this.stats.saves(), "nothing dirty, nothing written");
        service.close();
    }

    @Test
    public void shutdownFlushWritesEveryKeyAndStopsTimers() {
        InMemoryStore<UUID, Profile> syncProfiles = Stores.inMemory();
        InMemoryStore<UUID, Stats> syncStats = Stores.inMemory();
        PlayerDataService service = PlayerDataService.create(PlayerDataOptions.defaults().withLoadTimeout(LOAD_TIMEOUT), this.clock);
        service.register(PROFILE, syncProfiles);
        service.register(STATS, syncStats);
        TestServiceContext context = new TestServiceContext();
        service.enable(context);
        assertEquals(2, this.scheduler.pendingScheduled(), "autosave and sweep timers");

        service.preload(this.id);
        service.join(this.player.fake());
        assertTrue(service.isLoaded(this.id));
        service.set(this.id, PROFILE, new Profile("will", 9));
        service.require(this.id, STATS).kills = 4; // not marked dirty on purpose

        service.close();
        assertTrue(service.isClosed());
        assertEquals(0, this.scheduler.pendingScheduled());
        assertEquals(new Profile("will", 9), syncProfiles.load(this.id).join().orElseThrow());
        assertEquals(4, syncStats.load(this.id).join().orElseThrow().kills, "shutdown writes clean keys too");
        assertFalse(service.isLoaded(this.id));
        assertThrows(ApiMisuseException.class, () -> service.preload(this.id));
    }

    @Test
    public void shutdownLogsKeysThatDidNotFlushWithinTheTimeout() {
        PlayerDataService service = PlayerDataService.create(PlayerDataOptions.defaults()
                .withLoadTimeout(LOAD_TIMEOUT)
                .withFlushTimeout(Duration.ofMillis(50)), this.clock);
        service.register(PROFILE, this.profiles);
        loadAndJoin(service);

        // the async backend never runs, so the flush cannot complete; close must still return
        service.close();
        assertTrue(service.isClosed());
        assertEquals(0, this.profiles.saves());
    }

    // ---- registration ----

    @Test
    public void lateRegistrationLoadsTheKeyForPlayersAlreadyOnline() {
        PlayerDataService service = PlayerDataService.create(PlayerDataOptions.defaults().withLoadTimeout(LOAD_TIMEOUT), this.clock);
        service.register(PROFILE, this.profiles);
        loadAndJoin(service);
        assertTrue(service.get(this.id, STATS).isEmpty());

        service.register(STATS, this.stats);
        assertTrue(service.get(this.id, STATS).isEmpty(), "not present until its load lands");
        this.scheduler.runAll();
        assertTrue(service.get(this.id, STATS).isPresent());
        assertTrue(service.isLoaded(this.id));
    }

    @Test
    public void duplicateKeyRegistrationIsMisuse() {
        PlayerDataService service = service(JoinPolicy.KICK);
        assertThrows(ApiMisuseException.class, () -> service.register(PROFILE, this.profiles));
        assertEquals(java.util.Set.of(PROFILE, STATS), service.keys());
    }

    @Test
    public void keysAreEqualByIdAndType() {
        assertEquals(PROFILE, PlayerDataKey.of("profile", Profile.class, () -> new Profile("other", 1)));
        assertFalse(PROFILE.equals(PlayerDataKey.of("profile", Stats.class, Stats::new)));
        assertThrows(ApiMisuseException.class, () -> PlayerDataKey.of(" ", Profile.class, () -> null));
    }

    @Test
    public void optionsRejectNonsense() {
        assertThrows(ApiMisuseException.class, () -> PlayerDataOptions.defaults().withLoadTimeout(Duration.ZERO));
        assertThrows(ApiMisuseException.class, () -> PlayerDataOptions.defaults().withAutosaveInterval(Duration.ofSeconds(-1)));
        assertEquals(JoinPolicy.DEFER, PlayerDataOptions.defaults().withJoinPolicy(JoinPolicy.DEFER).joinPolicy());
    }

    // ---- fixtures ----

    record Profile(String name, int level) {
    }

    static final class Stats {
        int kills;
        final List<String> log = new ArrayList<>();

        Stats copy() {
            Stats copy = new Stats();
            copy.kills = this.kills;
            copy.log.addAll(this.log);
            return copy;
        }

        @Override
        public String toString() {
            return "Stats[kills=" + this.kills + "]";
        }
    }
}
