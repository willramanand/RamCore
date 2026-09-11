package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.config.ConfigKey;
import dev.willram.ramcore.config.TypedConfig;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Connection settings for a pooled {@link SqlStore}.
 *
 * @param jdbcUrl  the JDBC URL
 * @param username the user, null for URL-embedded or none (SQLite)
 * @param password the password, null for none
 * @param poolSize maximum pooled connections
 * @param dialect  the SQL dialect
 */
public record SqlStoreConfig(@NotNull String jdbcUrl,
                             @Nullable String username,
                             @Nullable String password,
                             int poolSize,
                             @NotNull SqlDialect dialect) {

    public static final int DEFAULT_POOL_SIZE = 8;

    public SqlStoreConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(dialect, "dialect");
        RamPreconditions.checkArgument(poolSize >= 1, "poolSize must be >= 1: " + poolSize, "Use at least one connection.");
    }

    /**
     * SQLite database file. Pool size 1: SQLite serialises writers anyway.
     *
     * @param file the database file
     * @return the config
     */
    @NotNull
    public static SqlStoreConfig sqlite(@NotNull Path file) {
        Objects.requireNonNull(file, "file");
        return new SqlStoreConfig("jdbc:sqlite:" + file.toAbsolutePath(), null, null, 1, SqlDialect.SQLITE);
    }

    /**
     * Any JDBC URL with the dialect detected from its prefix.
     *
     * @param jdbcUrl  the URL
     * @param username the user, may be null
     * @param password the password, may be null
     * @return the config
     */
    @NotNull
    public static SqlStoreConfig of(@NotNull String jdbcUrl, @Nullable String username, @Nullable String password) {
        return new SqlStoreConfig(jdbcUrl, username, password, DEFAULT_POOL_SIZE, SqlDialect.fromJdbcUrl(jdbcUrl));
    }

    @NotNull
    public SqlStoreConfig withPoolSize(int poolSize) {
        return new SqlStoreConfig(this.jdbcUrl, this.username, this.password, poolSize, this.dialect);
    }

    // ---- config file binding ----

    /**
     * The config keys under a prefix, for registering with {@code BukkitConfig.load(path, keys)}:
     * {@code <prefix>.url}, {@code .username}, {@code .password}, {@code .pool-size}.
     *
     * @param prefix the key prefix, for example {@code storage.sql}
     * @return the keys
     */
    @NotNull
    public static List<ConfigKey<?>> configKeys(@NotNull String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return List.of(
                ConfigKey.of(prefix + ".url", String.class, "jdbc:sqlite:plugins/RamCore/storage.db"),
                ConfigKey.of(prefix + ".username", String.class, ""),
                ConfigKey.of(prefix + ".password", String.class, ""),
                ConfigKey.of(prefix + ".pool-size", Integer.class, DEFAULT_POOL_SIZE)
                        .validate(size -> size >= 1, "must be >= 1")
        );
    }

    /**
     * Reads a config registered with {@link #configKeys(String)}. Blank username/password become null.
     *
     * @param config the loaded config
     * @param prefix the key prefix used when registering
     * @return the store config
     */
    @NotNull
    public static SqlStoreConfig fromConfig(@NotNull TypedConfig config, @NotNull String prefix) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(prefix, "prefix");
        String url = config.get(ConfigKey.of(prefix + ".url", String.class, ""));
        String username = blankToNull(config.get(ConfigKey.of(prefix + ".username", String.class, "")));
        String password = blankToNull(config.get(ConfigKey.of(prefix + ".password", String.class, "")));
        int poolSize = config.get(ConfigKey.of(prefix + ".pool-size", Integer.class, DEFAULT_POOL_SIZE));
        return new SqlStoreConfig(url, username, password, poolSize, SqlDialect.fromJdbcUrl(url));
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public String toString() {
        return "SqlStoreConfig[" + this.dialect + " " + this.jdbcUrl + " user=" + this.username + " pool=" + this.poolSize + "]";
    }
}
