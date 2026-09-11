package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.content.ContentKey;
import dev.willram.ramcore.content.ContentRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Registry of {@link Dialogue}s, backed by a {@link ContentRegistry}. Owner-scoped.
 */
public final class DialogueRegistry implements AutoCloseable {
    private final ContentRegistry<Dialogue> dialogues = ContentRegistry.create(Dialogue.class);

    @NotNull
    public Dialogue register(@NotNull String owner, @NotNull Dialogue dialogue) {
        ContentKey<Dialogue> key = ContentKey.of(dialogue.id(), Dialogue.class);
        return this.dialogues.register(owner, key, dialogue).value();
    }

    @NotNull
    public Optional<Dialogue> get(@NotNull ContentId id) {
        return this.dialogues.get(id);
    }

    @NotNull
    public Dialogue require(@NotNull ContentId id) {
        return this.dialogues.require(id);
    }

    public boolean contains(@NotNull ContentId id) {
        return this.dialogues.contains(id);
    }

    @NotNull
    public Set<ContentId> ids() {
        return this.dialogues.ids();
    }

    public int unregisterOwner(@NotNull String owner) {
        return this.dialogues.unregisterOwner(owner);
    }

    @Override
    public void close() {
        this.dialogues.close();
    }
}
