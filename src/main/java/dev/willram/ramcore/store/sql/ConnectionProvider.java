package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.terminable.Terminable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Hands out JDBC connections to {@link SqlStore}. The store closes each connection after use, so a
 * pooled provider returns it to the pool and a plain provider closes it.
 */
public interface ConnectionProvider extends Terminable {

    /**
     * Opens or borrows a connection. The caller closes it.
     *
     * @return a connection
     * @throws SQLException when none can be obtained
     */
    @NotNull
    Connection connection() throws SQLException;

    /**
     * Releases the provider's resources (closes the pool).
     */
    @Override
    void close();

    /**
     * A provider that opens a new {@link DriverManager} connection per call. No pooling; fine for
     * SQLite files and tests, not for a busy remote database.
     *
     * @param jdbcUrl  the URL
     * @param username the user, may be null
     * @param password the password, may be null
     * @return the provider
     */
    @NotNull
    static ConnectionProvider driverManager(@NotNull String jdbcUrl, @Nullable String username, @Nullable String password) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        return new ConnectionProvider() {
            @Override
            public @NotNull Connection connection() throws SQLException {
                if (username == null) {
                    return DriverManager.getConnection(jdbcUrl);
                }
                return DriverManager.getConnection(jdbcUrl, username, password);
            }

            @Override
            public void close() {
            }

            @Override
            public String toString() {
                return "DriverManagerProvider[" + jdbcUrl + "]";
            }
        };
    }

    /**
     * A HikariCP pool for the given configuration. HikariCP is an optional dependency resolved at
     * runtime; this throws an actionable exception when it is not on the classpath.
     *
     * @param config the connection configuration
     * @return the pooled provider
     * @throws dev.willram.ramcore.exception.ApiMisuseException when HikariCP is unavailable
     */
    @NotNull
    static ConnectionProvider hikari(@NotNull SqlStoreConfig config) {
        return HikariConnectionProviders.open(config);
    }
}
