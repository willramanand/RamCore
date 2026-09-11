package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.store.Store;
import dev.willram.ramcore.store.StoreCodec;
import dev.willram.ramcore.store.StoreContractTest;
import dev.willram.ramcore.store.StoreMigrations;
import dev.willram.ramcore.store.StoredRecord;
import dev.willram.ramcore.store.Stores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract run against an in-process SQLite file through the unpooled provider.
 */
public final class SqlStoreTest extends StoreContractTest {
    private static final StoreCodec<Profile> CODEC = StoreCodec.gson(Profile.class);

    @TempDir
    Path tempDir;

    private int counter;

    @Override
    protected Store<String, Profile> newStore(StoreMigrations<Profile> migrations) {
        String url = "jdbc:sqlite:" + this.tempDir.resolve("store-" + (this.counter++) + ".db").toAbsolutePath();
        return Stores.sql(ConnectionProvider.driverManager(url, null, null), SqlDialect.SQLITE, "profiles",
                DataKeyCodec.stringKeys(), CODEC, migrations);
    }

    @Override
    protected void seed(Store<String, Profile> store, String key, int version, Profile value) {
        SqlStore<String, Profile> sql = (SqlStore<String, Profile>) store;
        // go through the store once so the table exists, then write the raw row
        await(sql.keys());
        try (Connection connection = provider(sql).connection();
             PreparedStatement statement = connection.prepareStatement(SqlDialect.SQLITE.upsert("profiles"))) {
            statement.setString(1, key);
            statement.setInt(2, version);
            statement.setString(3, CODEC.encode(StoredRecord.of(version, value)));
            statement.setLong(4, 0L);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Optional<Integer> persistedVersion(Store<String, Profile> store, String key) {
        SqlStore<String, Profile> sql = (SqlStore<String, Profile>) store;
        try (Connection connection = provider(sql).connection();
             PreparedStatement statement = connection.prepareStatement("SELECT data_version FROM profiles WHERE store_key = ?")) {
            statement.setString(1, key);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getInt(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConnectionProvider provider(SqlStore<String, Profile> store) {
        // the test provider is stateless, so a fresh one over the same file sees the same data
        return ConnectionProvider.driverManager(jdbcUrlOf(store), null, null);
    }

    private String jdbcUrlOf(SqlStore<String, Profile> store) {
        // stores are created in newStore with counter-1
        return "jdbc:sqlite:" + this.tempDir.resolve("store-" + (this.counter - 1) + ".db").toAbsolutePath();
    }

    @Test
    public void tableIsCreatedOnFirstUseAndColumnVersionIsAuthoritative() throws SQLException {
        await(this.store.save("will", new Profile("will", 1)));
        SqlStore<String, Profile> sql = (SqlStore<String, Profile>) this.store;

        try (Connection connection = provider(sql).connection();
             PreparedStatement statement = connection.prepareStatement("UPDATE profiles SET data_version = 7 WHERE store_key = ?")) {
            statement.setString(1, "will");
            assertEquals(1, statement.executeUpdate());
        }

        assertEquals(Optional.of(7), persistedVersion(this.store, "will"));
        assertEquals(new Profile("will", 1), await(this.store.load("will")).orElseThrow(), "no migrations registered: value untouched");
    }

    @Test
    public void keysSurviveEncoding() {
        await(this.store.save("players/will", new Profile("will", 1)));

        assertEquals(Set.of("players/will"), await(this.store.keys()));
        assertTrue(await(this.store.load("players/will")).isPresent());
    }
}
