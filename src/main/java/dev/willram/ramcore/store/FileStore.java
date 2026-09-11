package dev.willram.ramcore.store;

import dev.willram.ramcore.data.DataKeyCodec;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One file per key under a directory, written atomically. Keys are turned into file names by a
 * {@link DataKeyCodec}; values by a {@link StoreCodec}. Using {@link StoreCodec#dataItem(Class)}
 * reads the files a legacy {@code FileDataRepository} wrote.
 *
 * <p>Stability: stable. Folia-safe by design; all I/O runs on the async scheduler.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class FileStore<K, V> extends AbstractAsyncStore<K, V> {
    public static final String DEFAULT_EXTENSION = ".json";

    private final Path directory;
    private final DataKeyCodec<K> keyCodec;
    private final StoreCodec<V> codec;
    private final String extension;

    public FileStore(@NotNull Path directory,
                     @NotNull DataKeyCodec<K> keyCodec,
                     @NotNull StoreCodec<V> codec,
                     @NotNull StoreMigrations<V> migrations) {
        this(directory, keyCodec, codec, migrations, DEFAULT_EXTENSION);
    }

    public FileStore(@NotNull Path directory,
                     @NotNull DataKeyCodec<K> keyCodec,
                     @NotNull StoreCodec<V> codec,
                     @NotNull StoreMigrations<V> migrations,
                     @NotNull String extension) {
        super(migrations);
        this.directory = Objects.requireNonNull(directory, "directory");
        this.keyCodec = Objects.requireNonNull(keyCodec, "keyCodec");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.extension = Objects.requireNonNull(extension, "extension");
    }

    @NotNull
    public Path directory() {
        return this.directory;
    }

    /**
     * The file a key maps to.
     *
     * @param key the key
     * @return the file path
     */
    @NotNull
    public Path path(@NotNull K key) {
        return this.directory.resolve(this.keyCodec.encode(Objects.requireNonNull(key, "key")) + this.extension);
    }

    @NotNull
    @Override
    protected Optional<StoredRecord<V>> doLoad(@NotNull K key) throws IOException {
        Path path = path(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(this.codec.decode(StoreFiles.readString(path)));
    }

    @Override
    protected void doSave(@NotNull K key, @NotNull StoredRecord<V> record) throws IOException {
        StoreFiles.writeAtomically(path(key), this.codec.encode(record));
    }

    @Override
    protected boolean doDelete(@NotNull K key) throws IOException {
        return Files.deleteIfExists(path(key));
    }

    @NotNull
    @Override
    protected Set<K> doKeys() throws IOException {
        Set<K> keys = new LinkedHashSet<>();
        for (String name : StoreFiles.listNames(this.directory, this.extension)) {
            keys.add(this.keyCodec.decode(name));
        }
        return keys;
    }
}
