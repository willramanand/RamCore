package dev.willram.ramcore.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class BukkitConfigTest {
    private static final ConfigKey<String> PREFIX =
            ConfigKey.of("messages.prefix", String.class, "<gold>[RamCore]</gold>");
    private static final ConfigKey<Integer> RETRIES =
            ConfigKey.of("startup.retries", Integer.class, 3)
                    .validate(value -> value >= 0, "must be >= 0");
    private static final ConfigKey<String> DATABASE_URL =
            ConfigKey.required("database.url", String.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadWritesDefaults() throws Exception {
        Path path = this.temporaryFolder.newFolder().toPath().resolve("config.yml");

        BukkitConfig config = BukkitConfig.load(path, PREFIX, RETRIES);

        assertEquals("<gold>[RamCore]</gold>", config.get(PREFIX));
        assertEquals(Integer.valueOf(3), config.get(RETRIES));
        String yaml = Files.readString(path);
        assertTrue(yaml.contains("prefix: <gold>[RamCore]</gold>"));
        assertTrue(yaml.contains("retries: 3"));
    }

    @Test
    public void reloadReadsUpdatedValues() throws Exception {
        Path path = this.temporaryFolder.newFile("config.yml").toPath();
        Files.writeString(path, """
                messages:
                  prefix: <green>[Test]</green>
                startup:
                  retries: 5
                """);
        BukkitConfig config = BukkitConfig.load(path, PREFIX, RETRIES);

        Files.writeString(path, """
                messages:
                  prefix: <red>[Reload]</red>
                startup:
                  retries: 8
                """);

        config.reload();

        assertEquals("<red>[Reload]</red>", config.get(PREFIX));
        assertEquals(Integer.valueOf(8), config.get(RETRIES));
    }

    @Test
    public void validationReportsInvalidValues() throws Exception {
        Path path = this.temporaryFolder.newFile("config.yml").toPath();
        Files.writeString(path, """
                startup:
                  retries: -1
                """);

        try {
            BukkitConfig.load(path, RETRIES, DATABASE_URL);
        } catch (ConfigValidationException e) {
            assertEquals(2, e.errors().size());
            assertTrue(e.errors().contains("startup.retries: must be >= 0"));
            assertTrue(e.errors().contains("database.url: required value missing"));
            return;
        }

        throw new AssertionError("expected ConfigValidationException");
    }
}
