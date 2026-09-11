package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import dev.willram.ramcore.content.ContentRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Registry of {@link Stat} definitions, backed by a {@link ContentRegistry}. Owner-scoped so a
 * consumer plugin can unregister all of its stats at once.
 */
public final class StatRegistry implements AutoCloseable {
    private final ContentRegistry<Stat> stats = ContentRegistry.create(Stat.class);

    /**
     * Registers a stat under an owner.
     *
     * @param owner the owning plugin/module name
     * @param stat  the stat
     * @return the registered stat
     */
    @NotNull
    public Stat register(@NotNull String owner, @NotNull Stat stat) {
        ContentKey<Stat> key = ContentKey.of(stat.id(), Stat.class);
        return this.stats.register(owner, key, stat).value();
    }

    /**
     * A stat by id.
     *
     * @param id the stat id
     * @return the stat, or empty
     */
    @NotNull
    public Optional<Stat> get(@NotNull ContentId id) {
        return this.stats.get(id);
    }

    /**
     * A stat by id, or a misuse exception if absent.
     *
     * @param id the stat id
     * @return the stat
     */
    @NotNull
    public Stat require(@NotNull ContentId id) {
        return this.stats.require(id);
    }

    public boolean contains(@NotNull ContentId id) {
        return this.stats.contains(id);
    }

    @NotNull
    public Set<ContentId> ids() {
        return this.stats.ids();
    }

    public int unregisterOwner(@NotNull String owner) {
        return this.stats.unregisterOwner(owner);
    }

    @Override
    public void close() {
        this.stats.close();
    }
}
