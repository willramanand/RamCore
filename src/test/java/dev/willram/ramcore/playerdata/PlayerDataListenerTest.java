package dev.willram.ramcore.playerdata;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.willram.ramcore.store.InMemoryStore;
import dev.willram.ramcore.store.Stores;
import dev.willram.ramcore.testkit.FakeScheduler;
import dev.willram.ramcore.testkit.ProxyFakes;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link PlayerDataListener} with directly constructed Bukkit events.
 */
public final class PlayerDataListenerTest {
    private static final PlayerDataKey<String> NAME = PlayerDataKey.of("name", String.class, () -> "anon");

    private FakeScheduler scheduler;
    private InMemoryStore<UUID, String> store;
    private PlayerDataService service;
    private PlayerDataListener listener;
    private UUID id;
    private Player player;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.store = Stores.inMemory();
        this.service = PlayerDataService.create(PlayerDataOptions.defaults().withLoadTimeout(Duration.ofSeconds(1)));
        this.service.register(NAME, this.store);
        this.listener = new PlayerDataListener(this.service);
        this.id = UUID.randomUUID();
        this.player = ProxyFakes.proxy(Player.class, Map.of("getUniqueId", this.id, "getName", "will"));
    }

    @AfterEach
    void tearDown() {
        this.service.close();
        this.scheduler.close();
    }

    private AsyncPlayerPreLoginEvent preLogin() {
        InetAddress address = InetAddress.getLoopbackAddress();
        PlayerProfile profile = ProxyFakes.proxy(PlayerProfile.class, Map.of("getId", this.id, "getName", "will"));
        return new AsyncPlayerPreLoginEvent("will", address, address, this.id, false, profile);
    }

    @Test
    public void allowedPreLoginPreloads() {
        this.listener.onPreLogin(preLogin());
        assertTrue(this.service.isPending(this.id));
    }

    @Test
    public void disallowedPreLoginIsIgnored() {
        AsyncPlayerPreLoginEvent event = preLogin();
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text("banned"));
        this.listener.onPreLogin(event);
        assertFalse(this.service.isPending(this.id));
    }

    @Test
    public void disallowedLoginCancelsPending() {
        this.listener.onPreLogin(preLogin());
        PlayerLoginEvent login = new PlayerLoginEvent(this.player, "localhost", InetAddress.getLoopbackAddress());
        login.disallow(PlayerLoginEvent.Result.KICK_FULL, Component.text("full"));
        this.listener.onLogin(login);
        assertFalse(this.service.isPending(this.id));
    }

    @Test
    public void allowedLoginKeepsPending() {
        this.listener.onPreLogin(preLogin());
        this.listener.onLogin(new PlayerLoginEvent(this.player, "localhost", InetAddress.getLoopbackAddress()));
        assertTrue(this.service.isPending(this.id));
    }

    @Test
    public void joinPromotesAndQuitSavesThenEvicts() {
        this.store.save(this.id, "William");
        this.listener.onPreLogin(preLogin());
        this.listener.onJoin(new PlayerJoinEvent(this.player, (Component) null));
        assertEquals("William", this.service.require(this.player, NAME));

        this.service.set(this.player, NAME, "Will");
        this.listener.onQuit(new PlayerQuitEvent(this.player, (Component) null));
        assertFalse(this.service.isLoaded(this.id));
        assertEquals("Will", this.store.load(this.id).join().orElseThrow());
    }
}
