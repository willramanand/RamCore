package dev.willram.ramcore.placeholder;

import dev.willram.ramcore.cooldown.Cooldown;
import dev.willram.ramcore.cooldown.CooldownTracker;
import dev.willram.ramcore.cooldown.Cooldowns;
import dev.willram.ramcore.party.PartyId;
import dev.willram.ramcore.party.PartyManager;
import dev.willram.ramcore.testkit.ProxyFakes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PlaceholderRegistryTest {
    private final UUID uuid = UUID.randomUUID();
    private final OfflinePlayer player = ProxyFakes.proxy(OfflinePlayer.class, Map.of("getUniqueId", this.uuid));

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    public void resolveSplitsIdAndParams() {
        PlaceholderRegistry registry = PlaceholderRegistry.create();
        registry.register(provider("ramcore", (p, params) -> "got:" + params));

        assertEquals("got:party_size", registry.resolve(this.player, "ramcore_party_size").orElseThrow());
        assertEquals("got:", registry.resolve(this.player, "ramcore").orElseThrow(), "no underscore means empty params");
        assertTrue(registry.resolve(this.player, "unknown_x").isEmpty());
    }

    @Test
    public void tagResolverBridgesIntoMiniMessage() {
        PlaceholderRegistry registry = PlaceholderRegistry.create();
        registry.register(provider("ramcore", (p, params) -> params.equals("greeting") ? "hi there" : null));

        Component rendered = MiniMessage.miniMessage().deserialize(
                "<ramcore:'greeting'>", registry.tagResolver(this.player));
        assertEquals("hi there", plain(rendered));
    }

    @Test
    public void duplicateAndInvalidIdsRejected() {
        PlaceholderRegistry registry = PlaceholderRegistry.create();
        registry.register(provider("ramcore", (p, params) -> ""));
        assertThrows(dev.willram.ramcore.exception.ApiMisuseException.class, () -> registry.register(provider("ramcore", (p, params) -> "")));
        assertThrows(dev.willram.ramcore.exception.ApiMisuseException.class, () -> registry.register(provider("bad_id", (p, params) -> "")));
    }

    @Test
    public void builtinPartySize() {
        PartyManager parties = PartyManager.create();
        parties.createParty(PartyId.of("p"), this.uuid);
        PlaceholderProvider provider = RamCorePlaceholders.builder().parties(parties).build();

        assertEquals("1", provider.resolve(this.player, "party_size"));
        assertEquals("0", provider.resolve(ProxyFakes.proxy(OfflinePlayer.class, Map.of("getUniqueId", UUID.randomUUID())), "party_size"));
    }

    @Test
    public void builtinCooldownRemainingSeconds() {
        CooldownTracker<String> tracker = Cooldowns.tracker(Cooldown.of(10, TimeUnit.SECONDS));
        tracker.test("combat"); // start it
        PlaceholderProvider provider = RamCorePlaceholders.builder().cooldowns("pvp", tracker).build();

        long remaining = Long.parseLong(provider.resolve(this.player, "cooldown_pvp_combat"));
        assertTrue(remaining > 0 && remaining <= 10, "remaining seconds within window: " + remaining);
        assertEquals("0", provider.resolve(this.player, "cooldown_pvp_untouched"), "untested key has no remaining time");
    }

    @Test
    public void unknownParamsResolveToNull() {
        PlaceholderProvider provider = RamCorePlaceholders.builder().build();
        assertEquals(null, provider.resolve(this.player, "party_size"), "unwired party returns null");
        assertEquals(null, provider.resolve(this.player, "nonsense"));
    }

    private static PlaceholderProvider provider(String id, java.util.function.BiFunction<OfflinePlayer, String, String> fn) {
        return new PlaceholderProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String resolve(OfflinePlayer player, String params) {
                return fn.apply(player, params);
            }
        };
    }
}
