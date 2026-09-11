package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import dev.willram.ramcore.content.ContentRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Registry of {@link Ability} definitions, backed by a {@link ContentRegistry}. Owner-scoped so a
 * consumer plugin can unregister all of its abilities at once.
 */
public final class AbilityRegistry implements AutoCloseable {
    private final ContentRegistry<Ability> abilities = ContentRegistry.create(Ability.class);

    @NotNull
    public Ability register(@NotNull String owner, @NotNull Ability ability) {
        ContentKey<Ability> key = ContentKey.of(ability.id(), Ability.class);
        return this.abilities.register(owner, key, ability).value();
    }

    @NotNull
    public Optional<Ability> get(@NotNull ContentId id) {
        return this.abilities.get(id);
    }

    @NotNull
    public Ability require(@NotNull ContentId id) {
        return this.abilities.require(id);
    }

    public boolean contains(@NotNull ContentId id) {
        return this.abilities.contains(id);
    }

    @NotNull
    public Set<ContentId> ids() {
        return this.abilities.ids();
    }

    public int unregisterOwner(@NotNull String owner) {
        return this.abilities.unregisterOwner(owner);
    }

    @Override
    public void close() {
        this.abilities.close();
    }
}
