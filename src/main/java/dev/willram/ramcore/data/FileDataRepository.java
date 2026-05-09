package dev.willram.ramcore.data;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Simple JSON/file-backed repository with dirty tracking, migrations, and async save helpers.
 */
public class FileDataRepository<K, V extends DataItem> extends DataRepository<K, V> implements AutoCloseable {
    private static final String EXTENSION = ".json";

    private final Path directory;
    private final DataKeyCodec<K> keyCodec;
    private final DataSerializer<V> serializer;
    private final Executor saveExecutor;
    private final List<DataRepositoryMigration<V>> migrations = new ArrayList<>();
    private final List<CompletableFuture<Void>> pendingSaves = new ArrayList<>();

    public FileDataRepository(@NotNull Path directory,
                              @NotNull DataKeyCodec<K> keyCodec,
                              @NotNull DataSerializer<V> serializer,
                              @NotNull Executor saveExecutor) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.keyCodec = Objects.requireNonNull(keyCodec, "keyCodec");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.saveExecutor = Objects.requireNonNull(saveExecutor, "saveExecutor");
    }

    @NotNull
    public FileDataRepository<K, V> migrateTo(int targetVersion, @NotNull DataMigration<V> migration) {
        this.migrations.add(new DataRepositoryMigration<>(targetVersion, migration));
        this.migrations.sort(Comparator.comparingInt(DataRepositoryMigration::targetVersion));
        return this;
    }

    @Override
    public void setup() {
        this.registry.clear();
        try {
            Files.createDirectories(this.directory);
            try (var stream = Files.list(this.directory)) {
                for (Path path : stream
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(EXTENSION))
                        .toList()) {
                    K key = this.keyCodec.decode(stripExtension(path.getFileName().toString()));
                    V item = read(path);
                    item = migrate(item);
                    add(key, item);
                }
            }
        } catch (IOException e) {
            throw new DataRepositoryException("failed to load repository from " + this.directory, e);
        }
    }

    @Override
    public void saveAll() {
        for (K key : List.copyOf(this.registry.keySet())) {
            save(key);
        }
    }

    public void saveDirty() {
        for (K key : List.copyOf(this.registry.keySet())) {
            V item = require(key);
            if (item.dirty()) {
                save(key);
            }
        }
    }

    public void save(@NotNull K key) {
        V item = require(key);
        if (item.shouldNotSave()) {
            return;
        }
        item.setSaving(true);
        try {
            Files.createDirectories(this.directory);
            write(path(key), item);
            item.markClean();
        } catch (IOException e) {
            throw new DataRepositoryException("failed to save data item " + key, e);
        } finally {
            item.setSaving(false);
        }
    }

    @NotNull
    public CompletableFuture<Void> queueSave(@NotNull K key) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> save(key), this.saveExecutor);
        synchronized (this.pendingSaves) {
            this.pendingSaves.add(future);
        }
        future.whenComplete((result, error) -> {
            synchronized (this.pendingSaves) {
                this.pendingSaves.remove(future);
            }
        });
        return future;
    }

    @NotNull
    public List<CompletableFuture<Void>> queueSaveDirty() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (K key : List.copyOf(this.registry.keySet())) {
            if (require(key).dirty()) {
                futures.add(queueSave(key));
            }
        }
        return List.copyOf(futures);
    }

    public void flushQueuedSaves() {
        List<CompletableFuture<Void>> futures;
        synchronized (this.pendingSaves) {
            futures = List.copyOf(this.pendingSaves);
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    public void delete(@NotNull K key) {
        remove(key);
        try {
            Files.deleteIfExists(path(key));
        } catch (IOException e) {
            throw new DataRepositoryException("failed to delete data item " + key, e);
        }
    }

    @NotNull
    public Path path(@NotNull K key) {
        return this.directory.resolve(this.keyCodec.encode(key) + EXTENSION);
    }

    @Override
    public void close() {
        flushQueuedSaves();
        saveDirty();
    }

    private V read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return this.serializer.read(reader);
        }
    }

    private void write(Path path, V item) throws IOException {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            this.serializer.write(writer, item);
        }
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private V migrate(V item) {
        V current = item;
        for (DataRepositoryMigration<V> migration : this.migrations) {
            if (current.dataVersion() < migration.targetVersion()) {
                current = migration.apply(current);
                current.dataVersion(migration.targetVersion());
                current.markDirty();
            }
        }
        return current;
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - EXTENSION.length());
    }
}
