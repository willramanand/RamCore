package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Gate in front of the HikariCP-backed provider. Only this class is referenced by public API; the
 * class that actually imports {@code com.zaxxer.hikari} is loaded here after a presence check, so a
 * server without HikariCP gets a clear error instead of a {@code NoClassDefFoundError}.
 */
final class HikariConnectionProviders {
    private static final String HIKARI_CLASS = "com.zaxxer.hikari.HikariDataSource";

    private HikariConnectionProviders() {
    }

    @NotNull
    static ConnectionProvider open(@NotNull SqlStoreConfig config) {
        Objects.requireNonNull(config, "config");
        RamPreconditions.checkState(available(),
                "HikariCP is not on the classpath, so a pooled SQL store cannot be created",
                "Enable storage.sql in plugins/RamCore/config.yml so the plugin loader resolves HikariCP and the JDBC driver, "
                        + "or use ConnectionProvider.driverManager(..) for an unpooled connection.");
        return new HikariConnectionProvider(config);
    }

    static boolean available() {
        try {
            Class.forName(HIKARI_CLASS, false, HikariConnectionProviders.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
