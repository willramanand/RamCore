package dev.willram.ramcore.store;

import dev.willram.ramcore.exception.ApiMisuseException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StoreMigrationsTest {

    @Test
    public void emptyChainIsVersionOneAndNeverMigrates() {
        StoreMigrations<String> none = StoreMigrations.none();
        StoredRecord<String> record = StoredRecord.of("x");

        assertEquals(1, none.currentVersion());
        assertFalse(none.needsMigration(1));
        assertSame(record, none.apply(record));
    }

    @Test
    public void stepsRunInOrderFromTheRecordVersion() {
        List<String> log = new ArrayList<>();
        StoreMigrations<String> chain = StoreMigrations.<String>start()
                .to(2, (value, from) -> {
                    log.add("2<-" + from);
                    return value + "b";
                })
                .to(3, (value, from) -> {
                    log.add("3<-" + from);
                    return value + "c";
                })
                .to(4, (value, from) -> {
                    log.add("4<-" + from);
                    return value + "d";
                });

        StoredRecord<String> migrated = chain.apply(StoredRecord.of(2, "a"));

        assertEquals(4, chain.currentVersion());
        assertEquals(new StoredRecord<>(4, "acd"), migrated);
        assertEquals(List.of("3<-2", "4<-3"), log);
    }

    @Test
    public void currentVersionRecordIsReturnedUnchanged() {
        StoreMigrations<String> chain = StoreMigrations.<String>start().to(2, (value, from) -> value + "!");
        StoredRecord<String> record = StoredRecord.of(2, "a");

        assertSame(record, chain.apply(record));
    }

    @Test
    public void versionsMustAscend() {
        StoreMigrations<String> chain = StoreMigrations.<String>start().to(3, (value, from) -> value);

        assertThrows(ApiMisuseException.class, () -> chain.to(2, (value, from) -> value));
        assertThrows(ApiMisuseException.class, () -> chain.to(3, (value, from) -> value));
        assertThrows(ApiMisuseException.class, () -> StoreMigrations.<String>start().to(1, (value, from) -> value));
    }

    @Test
    public void chainsAreImmutable() {
        StoreMigrations<String> base = StoreMigrations.<String>start().to(2, (value, from) -> value);
        StoreMigrations<String> extended = base.to(3, (value, from) -> value);

        assertEquals(2, base.currentVersion());
        assertEquals(3, extended.currentVersion());
        assertTrue(extended.needsMigration(2));
    }
}
