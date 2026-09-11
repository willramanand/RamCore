package dev.willram.ramcore.dialogue;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders a dialogue node and its available choices to the player, wiring each choice back to
 * {@link DialogueSession#choose(int)}. Built-ins: {@link ChatDialoguePresenter} and
 * {@link MenuDialoguePresenter}.
 */
@FunctionalInterface
public interface DialoguePresenter {

    /**
     * Presents a node.
     *
     * @param session the session (call {@link DialogueSession#choose(int)} when a choice is picked)
     * @param node    the current node
     * @param choices the available choices (indices match {@code choose})
     */
    void present(@NotNull DialogueSession session, @NotNull DialogueNode node, @NotNull List<DialogueChoice> choices);
}
