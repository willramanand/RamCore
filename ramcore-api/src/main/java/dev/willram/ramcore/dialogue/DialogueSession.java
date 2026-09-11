package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.exception.RamPreconditions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * One player's walk through a {@link Dialogue}. {@link #start()} enters the start node; each
 * {@link #choose(int)} runs the choice's actions and advances, ending the dialogue when a choice has
 * no next node or a node offers no available choices. Presentation is delegated to a
 * {@link DialoguePresenter}, so traversal is testable with a recording presenter.
 *
 * <p>Runs on the player's thread (chat/menu events fire there).</p>
 */
public final class DialogueSession {
    private final Player player;
    private final Dialogue dialogue;
    private final DialogueContext context;
    private final DialoguePresenter presenter;

    private DialogueNode current;
    private boolean ended;

    private DialogueSession(@NotNull Player player, @NotNull Dialogue dialogue, @NotNull DialogueContext context,
                            @NotNull DialoguePresenter presenter) {
        this.player = requireNonNull(player, "player");
        this.dialogue = requireNonNull(dialogue, "dialogue");
        this.context = requireNonNull(context, "context");
        this.presenter = requireNonNull(presenter, "presenter");
    }

    /** A session with a custom presenter. */
    @NotNull
    public static DialogueSession create(@NotNull Player player, @NotNull Dialogue dialogue,
                                         @NotNull DialogueContext context, @NotNull DialoguePresenter presenter) {
        return new DialogueSession(player, dialogue, context, presenter);
    }

    /** A chat-presented session. */
    @NotNull
    public static DialogueSession chat(@NotNull Player player, @NotNull Dialogue dialogue) {
        return create(player, dialogue, DialogueContext.of(player), new ChatDialoguePresenter());
    }

    /** A menu-presented session with the given menu title. */
    @NotNull
    public static DialogueSession menu(@NotNull Player player, @NotNull Dialogue dialogue, @NotNull String title) {
        return create(player, dialogue, DialogueContext.of(player), new MenuDialoguePresenter(title));
    }

    @NotNull
    public Player player() {
        return this.player;
    }

    @NotNull
    public DialogueContext context() {
        return this.context;
    }

    /** The current node, or {@code null} before {@link #start()}. */
    public DialogueNode current() {
        return this.current;
    }

    public boolean ended() {
        return this.ended;
    }

    /** Enters the start node and presents it. */
    public void start() {
        enter(this.dialogue.start());
    }

    /**
     * Selects an available choice by index, runs its actions, and advances.
     *
     * @param index the index into the last-presented available choices
     */
    public void choose(int index) {
        RamPreconditions.checkState(!this.ended, "dialogue has ended", "start a new session to talk again");
        RamPreconditions.checkState(this.current != null, "dialogue not started", "call start() first");
        List<DialogueChoice> choices = this.current.availableChoices(this.context);
        RamPreconditions.checkArgument(index >= 0 && index < choices.size(),
                "choice index " + index + " out of range (0.." + (choices.size() - 1) + ")",
                "pass an index from the presented choices");

        DialogueChoice choice = choices.get(index);
        for (DialogueAction action : choice.actions()) {
            action.run(this.context);
        }
        if (choice.ends()) {
            this.ended = true;
            return;
        }
        enter(this.dialogue.node(choice.nextNodeId()).orElseThrow());
    }

    private void enter(@NotNull DialogueNode node) {
        this.current = node;
        for (DialogueAction action : node.actions()) {
            action.run(this.context);
        }
        List<DialogueChoice> choices = node.availableChoices(this.context);
        this.presenter.present(this, node, choices);
        if (choices.isEmpty()) {
            this.ended = true;
        }
    }
}
