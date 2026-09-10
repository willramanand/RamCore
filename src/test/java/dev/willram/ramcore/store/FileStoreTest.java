package dev.willram.ramcore.store;

import dev.willram.ramcore.data.DataKeyCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FileStoreTest extends StoreContractTest {
    private static final StoreCodec<Profile> CODEC = StoreCodec.gson(Profile.class);

    @TempDir
    Path tempDir;

    @Override
    protected Store<String, Profile> newStore(StoreMigrations<Profile> migrations) {
        return Stores.file(this.tempDir.resolve("profiles"), DataKeyCodec.stringKeys(), CODEC, migrations);
    }

    @Override
    protected void seed(Store<String, Profile> store, String key, int version, Profile value) {
        try {
            StoreFiles.writeAtomically(((FileStore<String, Profile>) store).path(key), CODEC.encode(StoredRecord.of(version, value)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Optional<Integer> persistedVersion(Store<String, Profile> store, String key) {
        Path path = ((FileStore<String, Profile>) store).path(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(CODEC.decode(StoreFiles.readString(path)).dataVersion());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void keysAreUrlEncodedFileNames() {
        await(this.store.save("players/will", new Profile("will", 1)));

        FileStore<String, Profile> fileStore = (FileStore<String, Profile>) this.store;
        assertTrue(Files.isRegularFile(fileStore.directory().resolve("players%2Fwill.json")));
        assertEquals(Set.of("players/will"), await(this.store.keys()));
    }

    @Test
    public void leftoverTempFilesAreIgnoredWhenListing() throws IOException {
        await(this.store.save("a", new Profile("a", 1)));
        FileStore<String, Profile> fileStore = (FileStore<String, Profile>) this.store;
        Files.writeString(fileStore.directory().resolve("b.json.tmp"), "{partial");

        assertEquals(Set.of("a"), await(this.store.keys()));
        assertFalse(Files.exists(fileStore.path("b")));
    }

    @Test
    public void corruptFileFailsTheLoadPromiseWithStoreException() throws IOException {
        FileStore<String, Profile> fileStore = (FileStore<String, Profile>) this.store;
        Files.createDirectories(fileStore.directory());
        Files.writeString(fileStore.path("bad"), "not json at all");

        var promise = this.store.load("bad");
        settle();

        assertTrue(promise.isDone());
        assertTrue(promise.toCompletableFuture().isCompletedExceptionally());
    }
}
