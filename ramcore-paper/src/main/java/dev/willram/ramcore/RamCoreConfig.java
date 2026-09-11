package dev.willram.ramcore;

import dev.willram.ramcore.config.BukkitConfig;
import dev.willram.ramcore.config.ConfigKey;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * RamCore's own {@code config.yml} keys and loader. Loaded in {@link RamCore#load()}.
 */
public final class RamCoreConfig {
    public static final ConfigKey<Boolean> METRICS_ENABLED = ConfigKey.of("metrics.enabled", Boolean.class, true);
    public static final ConfigKey<Boolean> UPDATE_ENABLED = ConfigKey.of("update-checker.enabled", Boolean.class, true);
    public static final ConfigKey<String> UPDATE_REPO = ConfigKey.of("update-checker.repo", String.class, "willramanand/RamCore");
    public static final ConfigKey<Boolean> TIMELINE_ENABLED = ConfigKey.of("diagnostics.timeline.enabled", Boolean.class, false);
    public static final ConfigKey<Boolean> SQL_ENABLED = ConfigKey.of("storage.sql.enabled", Boolean.class, false);
    public static final ConfigKey<String> SQL_URL = ConfigKey.of("storage.sql.url", String.class, "jdbc:sqlite:plugins/RamCore/data.db");
    public static final ConfigKey<String> SQL_USERNAME = ConfigKey.of("storage.sql.username", String.class, "");
    public static final ConfigKey<String> SQL_PASSWORD = ConfigKey.of("storage.sql.password", String.class, "");
    public static final ConfigKey<Integer> SQL_POOL_SIZE = ConfigKey.of("storage.sql.pool-size", Integer.class, 8);
    public static final ConfigKey<Boolean> REDIS_ENABLED = ConfigKey.of("messaging.redis.enabled", Boolean.class, false);
    public static final ConfigKey<String> REDIS_URI = ConfigKey.of("messaging.redis.uri", String.class, "redis://localhost:6379");

    private RamCoreConfig() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    @NotNull
    public static List<ConfigKey<?>> keys() {
        return List.of(
                METRICS_ENABLED, UPDATE_ENABLED, UPDATE_REPO, TIMELINE_ENABLED,
                SQL_ENABLED, SQL_URL, SQL_USERNAME, SQL_PASSWORD, SQL_POOL_SIZE,
                REDIS_ENABLED, REDIS_URI
        );
    }

    /**
     * Loads (creating with defaults) {@code <dataFolder>/config.yml}.
     *
     * @param dataFolder the plugin data folder
     * @return the loaded config
     */
    @NotNull
    public static BukkitConfig load(@NotNull Path dataFolder) {
        return BukkitConfig.load(dataFolder.resolve("config.yml"), keys());
    }
}
