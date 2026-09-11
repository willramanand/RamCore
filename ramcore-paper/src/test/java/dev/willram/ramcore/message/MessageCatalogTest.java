package dev.willram.ramcore.message;

import dev.willram.ramcore.testkit.ProxyFakes;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MessageCatalogTest {
    private static final MessageKey WELCOME =
            MessageKey.of("welcome", "<green>Welcome, <player>!");
    private static final MessageKey ERROR =
            MessageKey.of("error", "<red><reason>");

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    // ---- existing behaviour (unchanged) ----

    @Test
    public void renderUsesPrefixAndPlaceholders() {
        MessageCatalog catalog = MessageCatalog.builder()
                .prefix("<gold>[RamCore]</gold> ")
                .build();

        assertEquals("[RamCore] Welcome, Steve!",
                plain(catalog.render(WELCOME, MessagePlaceholders.parsed("player", "Steve"))));
    }

    @Test
    public void configuredTemplateOverridesDefault() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(ERROR, "<red>Failed: <reason>")
                .build();

        assertEquals("Failed: <bad>",
                plain(catalog.render(ERROR, MessagePlaceholders.unparsed("reason", "<bad>"))));
        assertTrue(catalog.configuredTemplate(ERROR).isPresent());
    }

    @Test
    public void rawRenderingOmitsPrefix() {
        MessageCatalog catalog = MessageCatalog.builder()
                .prefix("<gold>[RamCore]</gold> ")
                .build();

        assertEquals("Welcome, Alex!",
                plain(catalog.renderRaw(WELCOME, MessagePlaceholders.parsed("player", "Alex"))));
    }

    // ---- locales ----

    @Test
    public void exactLocaleWins() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(WELCOME, "<green>Welcome, <player>!")
                .locale(Locale.GERMANY, Map.of(WELCOME, "Willkommen, <player>!"))
                .build();

        assertEquals("Willkommen, Steve!",
                plain(catalog.render(Locale.GERMANY, WELCOME, MessagePlaceholders.parsed("player", "Steve"))));
    }

    @Test
    public void countryFallsBackToLanguageOnly() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(WELCOME, "<green>Welcome, <player>!")
                .locale(Locale.of("de"), Map.of(WELCOME, "Willkommen, <player>!"))
                .build();

        // de_CH is not registered; de is
        assertEquals("Willkommen, Anna!",
                plain(catalog.render(Locale.of("de", "CH"), WELCOME, MessagePlaceholders.parsed("player", "Anna"))));
    }

    @Test
    public void unknownLocaleFallsBackToDefaultLocale() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(WELCOME, "<green>Welcome, <player>!")
                .locale(Locale.GERMANY, Map.of(WELCOME, "Willkommen, <player>!"))
                .build();

        assertEquals("Welcome, Pierre!",
                plain(catalog.render(Locale.FRENCH, WELCOME, MessagePlaceholders.parsed("player", "Pierre"))));
    }

    @Test
    public void missingEverywhereFallsBackToKeyDefaultTemplate() {
        MessageCatalog catalog = MessageCatalog.builder().build();
        assertEquals("Welcome, Bob!",
                plain(catalog.render(Locale.ITALIAN, WELCOME, MessagePlaceholders.parsed("player", "Bob"))));
    }

    @Test
    public void blankDefaultTemplateFallsBackToKeyId() {
        MessageKey blank = MessageKey.of("menu.title", "");
        MessageCatalog catalog = MessageCatalog.builder().build();
        assertEquals("menu.title", plain(catalog.renderRaw(blank)));
    }

    @Test
    public void nonDefaultDefaultLocaleReceivesMessageEntries() {
        MessageCatalog catalog = MessageCatalog.builder()
                .defaultLocale(Locale.GERMANY)
                .message(WELCOME, "Willkommen, <player>!")
                .build();

        assertEquals(Locale.GERMANY, catalog.defaultLocale());
        // render() with no locale uses the default locale
        assertEquals("Willkommen, Max!",
                plain(catalog.render(WELCOME, MessagePlaceholders.parsed("player", "Max"))));
    }

    @Test
    public void messageOverridesLocaleEntryForDefaultLocale() {
        MessageCatalog catalog = MessageCatalog.builder()
                .locale(Locale.US, Map.of(WELCOME, "From locale map"))
                .message(WELCOME, "From message()")
                .build();
        assertEquals("From message()", plain(catalog.renderRaw(Locale.US, WELCOME)));
    }

    // ---- send resolves the audience locale ----

    @Test
    public void sendResolvesPlayerLocaleByDefault() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(WELCOME, "<green>Welcome, <player>!")
                .locale(Locale.GERMANY, Map.of(WELCOME, "Willkommen, <player>!"))
                .build();

        assertEquals("Willkommen, Greta!",
                plain(sendCapturing(catalog, playerIn(Locale.GERMANY), MessagePlaceholders.parsed("player", "Greta"))));
    }

    @Test
    public void sendUsesDefaultLocaleForNonPlayerAudience() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(WELCOME, "<green>Welcome, <player>!")
                .locale(Locale.GERMANY, Map.of(WELCOME, "Willkommen, <player>!"))
                .build();

        AtomicReference<Component> captured = new AtomicReference<>();
        Audience console = ProxyFakes.proxy(Audience.class, Map.of(
                "sendMessage", (java.util.function.Function<Object[], Object>) args -> {
                    captured.set((Component) args[0]);
                    return null;
                }));
        catalog.send(console, WELCOME, MessagePlaceholders.parsed("player", "Server"));
        assertEquals("Welcome, Server!", plain(captured.get()));
    }

    @Test
    public void localeResolverOverrideIsHonoured() {
        MessageCatalog catalog = MessageCatalog.builder()
                .localeResolver(LocaleResolver.fixed(Locale.GERMANY))
                .message(WELCOME, "<green>Welcome, <player>!")
                .locale(Locale.GERMANY, Map.of(WELCOME, "Willkommen, <player>!"))
                .build();

        // a non-player audience still gets German because the resolver is fixed
        assertEquals("Willkommen, Ivan!",
                plain(sendCapturing(catalog, playerIn(Locale.US), MessagePlaceholders.parsed("player", "Ivan"))));
    }

    @Test
    public void noLocalesRegisteredMatchesLegacyBehaviour() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(WELCOME, "<green>Welcome, <player>!")
                .build();

        // player in German still gets the default template: nothing else is registered
        assertEquals("Welcome, Otto!",
                plain(sendCapturing(catalog, playerIn(Locale.GERMANY), MessagePlaceholders.parsed("player", "Otto"))));
    }

    private static Player playerIn(Locale locale) {
        return ProxyFakes.proxy(Player.class, Map.of("locale", locale));
    }

    private static Component sendCapturing(MessageCatalog catalog, Player player, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver placeholder) {
        AtomicReference<Component> captured = new AtomicReference<>();
        Player recording = ProxyFakes.proxy(Player.class, Map.of(
                "locale", player.locale(),
                "sendMessage", (java.util.function.Function<Object[], Object>) args -> {
                    captured.set((Component) args[0]);
                    return null;
                }));
        catalog.send(recording, WELCOME, placeholder);
        return captured.get();
    }
}
