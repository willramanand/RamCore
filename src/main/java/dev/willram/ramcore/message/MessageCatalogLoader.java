package dev.willram.ramcore.message;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Loads message templates from YAML files into a {@link Bundle}.
 *
 * <p>Layout under a directory: {@code <baseName>.yml} is the default locale, and every
 * {@code <baseName>_<tag>.yml} is a translated set. The tag is a locale tag with underscores, for
 * example {@code messages_en_US.yml} or {@code messages_de_DE.yml}; it is parsed with
 * {@link Locale#forLanguageTag(String)} after replacing {@code _} with {@code -}. Nested YAML keys
 * flatten to dotted ids, so a value at {@code command.error} becomes the {@link MessageKey} id
 * {@code command.error}.</p>
 *
 * <p>Because {@link MessageKey} equality is by id, the keys built here match the constants a
 * consumer passes to {@link MessageCatalog#render}. Uses Bukkit's {@link YamlConfiguration} for the
 * same parsing as the rest of RamCore's config.</p>
 *
 * <p>Stability: stable. Not Folia-thread-sensitive, but does blocking file I/O: load off the main
 * thread or during plugin load.</p>
 */
public final class MessageCatalogLoader {

    private MessageCatalogLoader() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Loads {@code messages.yml} and {@code messages_<tag>.yml} from a directory, with the default
     * locale {@link Locale#US}.
     *
     * @param directory the directory to scan
     * @return the loaded bundle
     */
    @NotNull
    public static Bundle yaml(@NotNull Path directory) {
        return yaml(directory, "messages", Locale.US);
    }

    /**
     * Loads {@code <baseName>.yml} and {@code <baseName>_<tag>.yml} from a directory, with the
     * default locale {@link Locale#US}.
     *
     * @param directory the directory to scan
     * @param baseName  the file base name, for example {@code messages}
     * @return the loaded bundle
     */
    @NotNull
    public static Bundle yaml(@NotNull Path directory, @NotNull String baseName) {
        return yaml(directory, baseName, Locale.US);
    }

    /**
     * Loads {@code <baseName>.yml} and {@code <baseName>_<tag>.yml} from a directory.
     *
     * @param directory     the directory to scan
     * @param baseName      the file base name, for example {@code messages}
     * @param defaultLocale the locale the base file is stored under
     * @return the loaded bundle
     */
    @NotNull
    public static Bundle yaml(@NotNull Path directory, @NotNull String baseName, @NotNull Locale defaultLocale) {
        requireNonNull(directory, "directory");
        requireNonNull(baseName, "baseName");
        requireNonNull(defaultLocale, "defaultLocale");

        Map<Locale, Map<MessageKey, String>> byLocale = new LinkedHashMap<>();
        Path base = directory.resolve(baseName + ".yml");
        if (Files.isRegularFile(base)) {
            byLocale.put(defaultLocale, read(base));
        }

        String prefix = baseName + "_";
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        if (!name.startsWith(prefix) || !name.endsWith(".yml")) {
                            return;
                        }
                        String tag = name.substring(prefix.length(), name.length() - ".yml".length());
                        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
                        if (locale.getLanguage().isEmpty()) {
                            throw new MessageLoadException("unparseable locale tag in " + name + ": '" + tag + "'");
                        }
                        byLocale.computeIfAbsent(locale, ignored -> new LinkedHashMap<>()).putAll(read(path));
                    });
        } catch (IOException e) {
            throw new MessageLoadException("failed to list message files in " + directory, e);
        }

        return new Bundle(defaultLocale, byLocale);
    }

    @NotNull
    private static Map<MessageKey, String> read(@NotNull Path file) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file.toFile());
        } catch (IOException | InvalidConfigurationException e) {
            throw new MessageLoadException("failed to load message file " + file, e);
        }
        return flatten(yaml);
    }

    @NotNull
    private static Map<MessageKey, String> flatten(@NotNull YamlConfiguration yaml) {
        Map<MessageKey, String> templates = new LinkedHashMap<>();
        for (String path : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(path)) {
                continue;
            }
            String template = yaml.getString(path);
            if (template != null) {
                templates.put(MessageKey.of(path, template), template);
            }
        }
        return templates;
    }

    /**
     * Copies bundled resource files into a directory on first run, skipping any that already exist.
     * Pass the resource names as they appear in the plugin jar, for example {@code messages.yml},
     * {@code messages_de_DE.yml}.
     *
     * @param plugin    the plugin whose jar holds the resources
     * @param directory the destination directory (created if absent)
     * @param resources the resource file names to copy
     * @return the number of files copied
     */
    public static int copyDefaults(@NotNull Plugin plugin, @NotNull Path directory, @NotNull String... resources) {
        requireNonNull(plugin, "plugin");
        requireNonNull(directory, "directory");
        requireNonNull(resources, "resources");

        int copied = 0;
        try {
            Files.createDirectories(directory);
            for (String resource : resources) {
                Path destination = directory.resolve(resource);
                if (Files.exists(destination)) {
                    continue;
                }
                try (InputStream in = plugin.getResource(resource)) {
                    if (in == null) {
                        continue;
                    }
                    Files.copy(in, destination);
                    copied++;
                }
            }
        } catch (IOException e) {
            throw new MessageLoadException("failed to copy default message files to " + directory, e);
        }
        return copied;
    }

    /**
     * The result of a load: a default locale and the templates found per locale. Apply it to a
     * {@link MessageCatalog.Builder} with {@link MessageCatalog.Builder#load(Bundle)}.
     *
     * @param defaultLocale the locale the base file was stored under
     * @param byLocale      templates per locale (unmodifiable)
     */
    public record Bundle(@NotNull Locale defaultLocale, @NotNull Map<Locale, Map<MessageKey, String>> byLocale) {

        public Bundle(@NotNull Locale defaultLocale, @NotNull Map<Locale, Map<MessageKey, String>> byLocale) {
            this.defaultLocale = requireNonNull(defaultLocale, "defaultLocale");
            Map<Locale, Map<MessageKey, String>> copy = new LinkedHashMap<>();
            requireNonNull(byLocale, "byLocale").forEach((locale, templates) -> copy.put(locale, Map.copyOf(templates)));
            this.byLocale = Map.copyOf(copy);
        }

        void applyTo(@NotNull MessageCatalog.Builder builder) {
            builder.defaultLocale(this.defaultLocale);
            Map<MessageKey, String> defaults = this.byLocale.get(this.defaultLocale);
            if (defaults != null) {
                builder.messages(defaults);
            }
            this.byLocale.forEach((locale, templates) -> {
                if (!locale.equals(this.defaultLocale)) {
                    builder.locale(locale, templates);
                }
            });
        }
    }

    /**
     * Thrown when message files cannot be read or a locale tag is unparseable.
     */
    public static final class MessageLoadException extends RuntimeException {
        public MessageLoadException(@NotNull String message) {
            super(message);
        }

        public MessageLoadException(@NotNull String message, @NotNull Throwable cause) {
            super(message, cause);
        }
    }
}
