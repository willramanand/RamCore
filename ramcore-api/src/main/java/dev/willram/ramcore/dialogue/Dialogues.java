package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.npc.NpcClickHandler;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Convenience attach points for dialogues.
 *
 * <p>For the objective link the roadmap describes ({@code ObjectiveTask RUN_ACTION} target
 * {@code dialogue:<id>}), add a node/choice {@link DialogueActions#custom} action that fires an
 * {@code ObjectiveEvent} with action {@code RUN_ACTION} and target {@code "dialogue:" + id} into your
 * objective tracker.</p>
 */
public final class Dialogues {

    private Dialogues() {
    }

    /**
     * An NPC click handler that opens the dialogue in chat for the clicking player.
     *
     * @param dialogue the dialogue
     * @return the handler for {@code NpcSpec.onClick(..)}
     */
    @NotNull
    public static NpcClickHandler onClick(@NotNull Dialogue dialogue) {
        requireNonNull(dialogue, "dialogue");
        return context -> DialogueSession.chat(context.player(), dialogue).start();
    }

    /**
     * An NPC click handler that opens the dialogue as a menu for the clicking player.
     *
     * @param dialogue the dialogue
     * @param title    the menu title
     * @return the handler for {@code NpcSpec.onClick(..)}
     */
    @NotNull
    public static NpcClickHandler onClickMenu(@NotNull Dialogue dialogue, @NotNull String title) {
        requireNonNull(dialogue, "dialogue");
        requireNonNull(title, "title");
        return context -> DialogueSession.menu(context.player(), dialogue, title).start();
    }
}
