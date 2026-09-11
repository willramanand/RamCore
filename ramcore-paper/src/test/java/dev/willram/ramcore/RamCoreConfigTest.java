package dev.willram.ramcore;

import dev.willram.ramcore.config.BukkitConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RamCoreConfigTest {

    @Test
    public void writesDefaultsOnFirstLoad(@TempDir Path dir) {
        BukkitConfig config = RamCoreConfig.load(dir);

        assertTrue(config.get(RamCoreConfig.METRICS_ENABLED));
        assertTrue(config.get(RamCoreConfig.UPDATE_ENABLED));
        assertEquals("willramanand/RamCore", config.get(RamCoreConfig.UPDATE_REPO));
        assertFalse(config.get(RamCoreConfig.SQL_ENABLED));
        assertEquals(8, config.get(RamCoreConfig.SQL_POOL_SIZE));
        assertFalse(config.get(RamCoreConfig.REDIS_ENABLED));
        assertEquals("redis://localhost:6379", config.get(RamCoreConfig.REDIS_URI));
        assertTrue(Files.exists(dir.resolve("config.yml")), "config.yml written with defaults");
    }

    @Test
    public void readsOverriddenValues(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), String.join("\n",
                "metrics:",
                "  enabled: false",
                "update-checker:",
                "  enabled: true",
                "  repo: someone/Fork",
                "") );
        BukkitConfig config = RamCoreConfig.load(dir);
        assertFalse(config.get(RamCoreConfig.METRICS_ENABLED));
        assertEquals("someone/Fork", config.get(RamCoreConfig.UPDATE_REPO));
    }
}
