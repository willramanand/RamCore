package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/**
 * SQL flavour differences for the key/value table used by {@link SqlStore}. Every dialect uses the
 * same columns: {@code store_key}, {@code data_version}, {@code data}, {@code updated_at}.
 */
public enum SqlDialect {
    SQLITE("jdbc:sqlite:", "TEXT", "INTEGER", "INTEGER") {
        @Override
        public @NotNull String upsert(@NotNull String table) {
            return "INSERT INTO " + quote(table) + " (store_key, data_version, data, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(store_key) DO UPDATE SET data_version = excluded.data_version, data = excluded.data, updated_at = excluded.updated_at";
        }
    },
    POSTGRESQL("jdbc:postgresql:", "TEXT", "INTEGER", "BIGINT") {
        @Override
        public @NotNull String upsert(@NotNull String table) {
            return "INSERT INTO " + quote(table) + " (store_key, data_version, data, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT (store_key) DO UPDATE SET data_version = EXCLUDED.data_version, data = EXCLUDED.data, updated_at = EXCLUDED.updated_at";
        }
    },
    MYSQL("jdbc:mysql:", "MEDIUMTEXT", "INT", "BIGINT") {
        @Override
        public @NotNull String upsert(@NotNull String table) {
            return "INSERT INTO " + quote(table) + " (store_key, data_version, data, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE data_version = VALUES(data_version), data = VALUES(data), updated_at = VALUES(updated_at)";
        }

        @Override
        @NotNull
        String quote(@NotNull String identifier) {
            return "`" + identifier + "`";
        }
    },
    MARIADB("jdbc:mariadb:", "MEDIUMTEXT", "INT", "BIGINT") {
        @Override
        public @NotNull String upsert(@NotNull String table) {
            return MYSQL.upsert(table);
        }

        @Override
        @NotNull
        String quote(@NotNull String identifier) {
            return MYSQL.quote(identifier);
        }
    };

    private final String urlPrefix;
    private final String textType;
    private final String intType;
    private final String longType;

    SqlDialect(String urlPrefix, String textType, String intType, String longType) {
        this.urlPrefix = urlPrefix;
        this.textType = textType;
        this.intType = intType;
        this.longType = longType;
    }

    /**
     * Detects the dialect from a JDBC URL prefix.
     *
     * @param jdbcUrl the URL
     * @return the dialect
     * @throws dev.willram.ramcore.exception.ApiMisuseException for an unsupported URL
     */
    @NotNull
    public static SqlDialect fromJdbcUrl(@NotNull String jdbcUrl) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        String lower = jdbcUrl.toLowerCase(Locale.ROOT);
        for (SqlDialect dialect : values()) {
            if (lower.startsWith(dialect.urlPrefix)) {
                return dialect;
            }
        }
        throw RamPreconditions.misuse("unsupported JDBC url: " + jdbcUrl,
                "Use a jdbc:sqlite:, jdbc:mysql:, jdbc:mariadb:, or jdbc:postgresql: url, or pass the SqlDialect explicitly.");
    }

    @NotNull
    public String urlPrefix() {
        return this.urlPrefix;
    }

    /** Validates a table name: letters, digits, underscore, starting with a letter or underscore. */
    @NotNull
    public static String validateTable(@NotNull String table) {
        Objects.requireNonNull(table, "table");
        RamPreconditions.checkArgument(table.matches("[A-Za-z_][A-Za-z0-9_]{0,63}"),
                "invalid SQL table name: " + table,
                "Use letters, digits and underscores only, for example 'player_profiles'.");
        return table;
    }

    @NotNull
    public String createTable(@NotNull String table) {
        return "CREATE TABLE IF NOT EXISTS " + quote(table) + " ("
                + "store_key VARCHAR(191) PRIMARY KEY, "
                + "data_version " + this.intType + " NOT NULL, "
                + "data " + this.textType + " NOT NULL, "
                + "updated_at " + this.longType + " NOT NULL)";
    }

    @NotNull
    public abstract String upsert(@NotNull String table);

    @NotNull
    public String select(@NotNull String table) {
        return "SELECT data_version, data FROM " + quote(table) + " WHERE store_key = ?";
    }

    @NotNull
    public String selectAll(@NotNull String table) {
        return "SELECT store_key, data_version, data FROM " + quote(table);
    }

    @NotNull
    public String selectKeys(@NotNull String table) {
        return "SELECT store_key FROM " + quote(table);
    }

    @NotNull
    public String delete(@NotNull String table) {
        return "DELETE FROM " + quote(table) + " WHERE store_key = ?";
    }

    @NotNull
    String quote(@NotNull String identifier) {
        return "\"" + identifier + "\"";
    }
}
