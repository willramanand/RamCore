package dev.willram.ramcore.dialogue;

import org.jetbrains.annotations.NotNull;

/**
 * Something a dialogue node or choice runs (send a message, pay a reward, open a menu, ...). Built-ins
 * are in {@link DialogueActions}.
 */
@FunctionalInterface
public interface DialogueAction {

    /** An action that does nothing. */
    DialogueAction NONE = context -> {
    };

    /**
     * Runs the action.
     *
     * @param context the dialogue context
     */
    void run(@NotNull DialogueContext context);
}
