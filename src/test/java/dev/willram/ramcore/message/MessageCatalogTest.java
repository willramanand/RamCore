package dev.willram.ramcore.message;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MessageCatalogTest {
    private static final MessageKey WELCOME =
            MessageKey.of("welcome", "<green>Welcome, <player>!");
    private static final MessageKey ERROR =
            MessageKey.of("error", "<red><reason>");

    @Test
    public void renderUsesPrefixAndPlaceholders() {
        MessageCatalog catalog = MessageCatalog.builder()
                .prefix("<gold>[RamCore]</gold> ")
                .build();

        String plain = PlainTextComponentSerializer.plainText().serialize(
                catalog.render(WELCOME, MessagePlaceholders.parsed("player", "Steve"))
        );

        assertEquals("[RamCore] Welcome, Steve!", plain);
    }

    @Test
    public void configuredTemplateOverridesDefault() {
        MessageCatalog catalog = MessageCatalog.builder()
                .message(ERROR, "<red>Failed: <reason>")
                .build();

        String plain = PlainTextComponentSerializer.plainText().serialize(
                catalog.render(ERROR, MessagePlaceholders.unparsed("reason", "<bad>"))
        );

        assertEquals("Failed: <bad>", plain);
        assertTrue(catalog.configuredTemplate(ERROR).isPresent());
    }

    @Test
    public void rawRenderingOmitsPrefix() {
        MessageCatalog catalog = MessageCatalog.builder()
                .prefix("<gold>[RamCore]</gold> ")
                .build();

        String plain = PlainTextComponentSerializer.plainText().serialize(
                catalog.renderRaw(WELCOME, MessagePlaceholders.parsed("player", "Alex"))
        );

        assertEquals("Welcome, Alex!", plain);
    }
}
