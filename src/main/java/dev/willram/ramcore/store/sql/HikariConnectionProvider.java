package dev.willram.ramcore.store.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP-backed provider. Loaded only through {@link HikariConnectionProviders} after the class
 * presence check.
 */
final class HikariConnectionProvider implements ConnectionProvider {
    private final HikariDataSource dataSource;

    HikariConnectionProvider(@NotNull SqlStoreConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.jdbcUrl());
        if (config.username() != null) {
            hikari.setUsername(config.username());
        }
        if (config.password() != null) {
            hikari.setPassword(config.password());
        }
        hikari.setMaximumPoolSize(config.poolSize());
        hikari.setPoolName("ramcore-" + config.dialect().name().toLowerCase(java.util.Locale.ROOT));
        this.dataSource = new HikariDataSource(hikari);
    }

    @NotNull
    @Override
    public Connection connection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    public void close() {
        this.dataSource.close();
    }

    @Override
    public boolean isClosed() {
        return this.dataSource.isClosed();
    }

    @Override
    public String toString() {
        return "HikariProvider[" + this.dataSource.getJdbcUrl() + "]";
    }
}
