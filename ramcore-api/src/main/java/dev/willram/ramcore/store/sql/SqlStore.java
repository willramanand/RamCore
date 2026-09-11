package dev.willram.ramcore.store.sql;

import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.store.AbstractAsyncStore;
import dev.willram.ramcore.store.StoreCodec;
import dev.willram.ramcore.store.StoreException;
import dev.willram.ramcore.store.StoreMigrations;
import dev.willram.ramcore.store.StoredRecord;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Key/value table in a SQL database: one row per key holding the encoded value and its schema
 * version. SQLite by default; MySQL, MariaDB and PostgreSQL through {@link SqlDialect}.
 *
 * <p>The table is created on first use. Every operation borrows a connection from the
 * {@link ConnectionProvider} and returns it. Closing the store closes the provider.</p>
 *
 * <p>Stability: experimental (SQLite is exercised in tests; the other dialects only at the SQL
 * string level). Folia-safe by design; all JDBC work runs on the async scheduler.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class SqlStore<K, V> extends AbstractAsyncStore<K, V> {
    private final ConnectionProvider connections;
    private final SqlDialect dialect;
    private final String table;
    private final DataKeyCodec<K> keyCodec;
    private final StoreCodec<V> codec;
    private volatile boolean schemaReady;

    public SqlStore(@NotNull ConnectionProvider connections,
                    @NotNull SqlDialect dialect,
                    @NotNull String table,
                    @NotNull DataKeyCodec<K> keyCodec,
                    @NotNull StoreCodec<V> codec,
                    @NotNull StoreMigrations<V> migrations) {
        super(migrations);
        this.connections = Objects.requireNonNull(connections, "connections");
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.table = SqlDialect.validateTable(table);
        this.keyCodec = Objects.requireNonNull(keyCodec, "keyCodec");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @NotNull
    public String table() {
        return this.table;
    }

    @NotNull
    public SqlDialect dialect() {
        return this.dialect;
    }

    @NotNull
    @Override
    protected Optional<StoredRecord<V>> doLoad(@NotNull K key) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(this.dialect.select(this.table))) {
            statement.setString(1, this.keyCodec.encode(key));
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                return Optional.of(decode(rows.getInt(1), rows.getString(2)));
            }
        }
    }

    @Override
    protected void doSave(@NotNull K key, @NotNull StoredRecord<V> record) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(this.dialect.upsert(this.table))) {
            statement.setString(1, this.keyCodec.encode(key));
            statement.setInt(2, record.dataVersion());
            statement.setString(3, this.codec.encode(record));
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    @Override
    protected boolean doDelete(@NotNull K key) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(this.dialect.delete(this.table))) {
            statement.setString(1, this.keyCodec.encode(key));
            return statement.executeUpdate() > 0;
        }
    }

    @NotNull
    @Override
    protected Set<K> doKeys() throws SQLException {
        Set<K> keys = new LinkedHashSet<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(this.dialect.selectKeys(this.table));
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                keys.add(this.keyCodec.decode(rows.getString(1)));
            }
        }
        return keys;
    }

    @NotNull
    @Override
    protected Map<K, StoredRecord<V>> doLoadAll() throws SQLException {
        Map<K, StoredRecord<V>> result = new LinkedHashMap<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(this.dialect.selectAll(this.table));
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.put(this.keyCodec.decode(rows.getString(1)), decode(rows.getInt(2), rows.getString(3)));
            }
        }
        return result;
    }

    @Override
    public void close() {
        this.connections.close();
    }

    @Override
    public boolean isClosed() {
        return this.connections.isClosed();
    }

    private StoredRecord<V> decode(int columnVersion, String data) {
        StoredRecord<V> record = this.codec.decode(data);
        // the column is authoritative for the version; the codec may not carry one (raw JSON)
        return record.dataVersion() == columnVersion ? record : new StoredRecord<>(Math.max(columnVersion, StoredRecord.INITIAL_VERSION), record.value());
    }

    private Connection open() throws SQLException {
        Connection connection = this.connections.connection();
        if (!this.schemaReady) {
            synchronized (this) {
                if (!this.schemaReady) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(this.dialect.createTable(this.table));
                    } catch (SQLException e) {
                        connection.close();
                        throw new StoreException("failed to create table " + this.table + " (" + this.dialect + ")", e);
                    }
                    this.schemaReady = true;
                }
            }
        }
        return connection;
    }
}
