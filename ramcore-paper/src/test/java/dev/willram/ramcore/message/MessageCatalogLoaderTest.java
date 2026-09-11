package dev.willram.ramcore.message;

import dev.willram.ramcore.testkit.ProxyFakes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MessageCatalogLoaderTest {
    private static final MessageKey WELCOME = MessageKey.of("welcome", "fallback");
    private static final MessageKey COMMAND_ERROR = MessageKey.of("command.error", "fallback");

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static void write(Path file, String... lines) throws Exception {
        Files.writeString(file, String.join("\n", lines) + "\n");
    }

    @Test
    public void loadsDefaultAndLocaleFilesWithNestedKeys(@TempDir Path dir) throws Exception {
        write(dir.resolve("messages.yml"),
                "welcome: '<green>Welcome, <player>!'",
                "command:",
                "  error: '<red>Nope'");
        write(dir.resolve("messages_de_DE.yml"),
                "welcome: 'Willkommen, <player>!'",
                "command:",
                "  error: '<red>Nein'");

        MessageCatalog catalog = MessageCatalog.builder()
                .load(MessageCatalogLoader.yaml(dir))
                .build();

        assertEquals(Locale.US, catalog.defaultLocale());
        assertEquals("Welcome, Sam!",
                plain(catalog.render(WELCOME, MessagePlaceholders.parsed("player", "Sam"))));
        assertEquals("Willkommen, Sam!",
                plain(catalog.render(Locale.GERMANY, WELCOME, MessagePlaceholders.parsed("player", "Sam"))));
        assertEquals("Nein", plain(catalog.renderRaw(Locale.GERMANY, COMMAND_ERROR)));
        // dotted id flattened from the nested section
        assertEquals("Nope", plain(catalog.renderRaw(Locale.FRENCH, COMMAND_ERROR)));
    }

    @Test
    public void customBaseNameAndDefaultLocale(@TempDir Path dir) throws Exception {
        write(dir.resolve("lang.yml"), "welcome: 'Willkommen'");
        write(dir.resolve("lang_en_US.yml"), "welcome: 'Welcome'");

        MessageCatalog catalog = MessageCatalog.builder()
                .load(MessageCatalogLoader.yaml(dir, "lang", Locale.GERMANY))
                .build();

        assertEquals(Locale.GERMANY, catalog.defaultLocale());
        assertEquals("Willkommen", plain(catalog.render(WELCOME)));
        assertEquals("Welcome", plain(catalog.renderRaw(Locale.US, WELCOME)));
    }

    @Test
    public void missingDirectoryFilesJustYieldEmptyBundle(@TempDir Path dir) {
        MessageCatalogLoader.Bundle bundle = MessageCatalogLoader.yaml(dir);
        assertTrue(bundle.byLocale().isEmpty());
        MessageCatalog catalog = MessageCatalog.builder().load(bundle).build();
        // falls back to the key's own default template
        assertEquals("fallback", plain(catalog.renderRaw(WELCOME)));
    }

    @Test
    public void unparseableLocaleTagThrows(@TempDir Path dir) throws Exception {
        write(dir.resolve("messages.yml"), "welcome: 'hi'");
        write(dir.resolve("messages_.yml"), "welcome: 'broken'");
        assertThrows(MessageCatalogLoader.MessageLoadException.class, () -> MessageCatalogLoader.yaml(dir));
    }

    @Test
    public void copyDefaultsWritesMissingFilesOnly(@TempDir Path dir) throws Exception {
        Path existing = dir.resolve("messages.yml");
        write(existing, "welcome: 'kept'");

        Plugin plugin = ProxyFakes.proxy(Plugin.class, Map.of(
                "getResource", (java.util.function.Function<Object[], Object>) args -> resource((String) args[0])));

        int copied = MessageCatalogLoader.copyDefaults(plugin, dir, "messages.yml", "messages_de_DE.yml", "absent.yml");

        assertEquals(1, copied, "only messages_de_DE.yml is copied: messages.yml exists, absent.yml has no resource");
        assertEquals("welcome: 'kept'\n", Files.readString(existing));
        assertEquals(List.of("welcome: 'Willkommen'"), Files.readAllLines(dir.resolve("messages_de_DE.yml")));
    }

    private static InputStream resource(String name) {
        return switch (name) {
            case "messages.yml" -> stream("welcome: 'from jar'");
            case "messages_de_DE.yml" -> stream("welcome: 'Willkommen'");
            default -> null;
        };
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
