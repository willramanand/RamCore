package dev.willram.ramcore.store;

import dev.willram.ramcore.promise.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Dirty tracking mixin for stores that hold a working set in memory.
 *
 * @param <K> key type
 */
public interface DirtyTracking<K> {

    /**
     * Marks a key as modified since its last save.
     *
     * @param key the key
     */
    void markDirty(@NotNull K key);

    /**
     * Checks whether a key is marked dirty.
     *
     * @param key the key
     * @return true if dirty
     */
    boolean dirty(@NotNull K key);

    /**
     * Snapshot of the dirty keys.
     *
     * @return dirty keys
     */
    @NotNull
    Set<K> dirtyKeys();

    /**
     * Clears the dirty mark without saving.
     *
     * @param key the key
     */
    void clearDirty(@NotNull K key);

    /**
     * Saves every dirty entry and clears its mark.
     *
     * @return number of entries saved
     */
    @NotNull
    Promise<Integer> saveDirty();
}
