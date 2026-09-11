package dev.willram.ramcore.dialogue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * A selectable option on a dialogue node.
 *
 * @param label      the MiniMessage label shown to the player
 * @param nextNodeId the node to go to when chosen, or {@code null} to end the dialogue
 * @param condition  when this choice is offered
 * @param actions    actions run when this choice is selected (before advancing)
 */
public record DialogueChoice(@NotNull String label, @Nullable String nextNodeId,
                             @NotNull Predicate<DialogueContext> condition,
                             @NotNull List<DialogueAction> actions) {

    public DialogueChoice {
        requireNonNull(label, "label");
        requireNonNull(condition, "condition");
        actions = List.copyOf(actions);
    }

    /** A choice that always shows and runs no actions. */
    @NotNull
    public static DialogueChoice of(@NotNull String label, @Nullable String nextNodeId) {
        return new DialogueChoice(label, nextNodeId, DialogueConditions.always(), List.of());
    }

    /** A choice with a condition. */
    @NotNull
    public static DialogueChoice of(@NotNull String label, @Nullable String nextNodeId,
                                    @NotNull Predicate<DialogueContext> condition) {
        return new DialogueChoice(label, nextNodeId, condition, List.of());
    }

    /** A copy of this choice with actions. */
    @NotNull
    public DialogueChoice withActions(@NotNull List<DialogueAction> actions) {
        return new DialogueChoice(this.label, this.nextNodeId, this.condition, actions);
    }

    /** Whether this choice ends the dialogue (no next node). */
    public boolean ends() {
        return this.nextNodeId == null;
    }
}
