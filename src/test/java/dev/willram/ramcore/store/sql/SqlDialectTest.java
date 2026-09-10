package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.exception.ApiMisuseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SqlDialectTest {

    @Test
    public void detectsDialectFromJdbcUrl() {
        assertEquals(SqlDialect.SQLITE, SqlDialect.fromJdbcUrl("jdbc:sqlite:/tmp/x.db"));
        assertEquals(SqlDialect.MYSQL, SqlDialect.fromJdbcUrl("JDBC:MYSQL://host/db"));
        assertEquals(SqlDialect.MARIADB, SqlDialect.fromJdbcUrl("jdbc:mariadb://host/db"));
        assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromJdbcUrl("jdbc:postgresql://host/db"));
        assertThrows(ApiMisuseException.class, () -> SqlDialect.fromJdbcUrl("jdbc:h2:mem:x"));
    }

    @Test
    public void upsertsUseEachEngineSyntax() {
        assertTrue(SqlDialect.SQLITE.upsert("t").contains("ON CONFLICT(store_key) DO UPDATE"));
        assertTrue(SqlDialect.POSTGRESQL.upsert("t").contains("ON CONFLICT (store_key) DO UPDATE"));
        assertTrue(SqlDialect.MYSQL.upsert("t").contains("ON DUPLICATE KEY UPDATE"));
        assertEquals(SqlDialect.MYSQL.upsert("t"), SqlDialect.MARIADB.upsert("t"));
    }

    @Test
    public void identifiersAreQuotedPerEngineAndColumnTypesDiffer() {
        assertTrue(SqlDialect.MYSQL.createTable("t").startsWith("CREATE TABLE IF NOT EXISTS `t`"));
        assertTrue(SqlDialect.MYSQL.createTable("t").contains("data MEDIUMTEXT"));
        assertTrue(SqlDialect.POSTGRESQL.createTable("t").startsWith("CREATE TABLE IF NOT EXISTS \"t\""));
        assertTrue(SqlDialect.POSTGRESQL.createTable("t").contains("updated_at BIGINT"));
        assertTrue(SqlDialect.SQLITE.createTable("t").contains("updated_at INTEGER"));
    }

    @Test
    public void tableNamesAreValidated() {
        assertEquals("player_profiles", SqlDialect.validateTable("player_profiles"));
        assertThrows(ApiMisuseException.class, () -> SqlDialect.validateTable("drop table; --"));
        assertThrows(ApiMisuseException.class, () -> SqlDialect.validateTable("1abc"));
    }

    @Test
    public void configDetectsDialectAndNormalisesBlankCredentials() {
        SqlStoreConfig config = SqlStoreConfig.of("jdbc:postgresql://db/ramcore", "ram", "secret").withPoolSize(4);

        assertEquals(SqlDialect.POSTGRESQL, config.dialect());
        assertEquals(4, config.poolSize());
        assertEquals(1, SqlStoreConfig.sqlite(java.nio.file.Path.of("x.db")).poolSize());
        assertEquals(4, SqlStoreConfig.configKeys("storage.sql").size());
    }
}
