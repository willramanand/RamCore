package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * One node in a {@link Dialogue}: the text shown, the choices offered, an availability condition, and
 * actions run when the node is entered.
 */
public final class DialogueNode {
    private final String id;
    private final String text;
    private final List<DialogueChoice> choices;
    private final Predicate<DialogueContext> condition;
    private final List<DialogueAction> actions;

    private DialogueNode(Builder builder) {
        this.id = RamPreconditions.notBlank(builder.id, "id");
        this.text = requireNonNull(builder.text, "text");
        this.choices = List.copyOf(builder.choices);
        this.condition = builder.condition;
        this.actions = List.copyOf(builder.actions);
    }

    @NotNull
    public static Builder builder(@NotNull String id, @NotNull String text) {
        return new Builder(id, text);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String text() {
        return this.text;
    }

    @NotNull
    public List<DialogueChoice> choices() {
        return this.choices;
    }

    @NotNull
    public Predicate<DialogueContext> condition() {
        return this.condition;
    }

    @NotNull
    public List<DialogueAction> actions() {
        return this.actions;
    }

    /** The choices available for the given context (condition passes). */
    @NotNull
    public List<DialogueChoice> availableChoices(@NotNull DialogueContext context) {
        List<DialogueChoice> available = new ArrayList<>();
        for (DialogueChoice choice : this.choices) {
            if (choice.condition().test(context)) {
                available.add(choice);
            }
        }
        return available;
    }

    public static final class Builder {
        private final String id;
        private final String text;
        private final List<DialogueChoice> choices = new ArrayList<>();
        private Predicate<DialogueContext> condition = DialogueConditions.always();
        private final List<DialogueAction> actions = new ArrayList<>();

        private Builder(@NotNull String id, @NotNull String text) {
            this.id = id;
            this.text = text;
        }

        @NotNull
        public Builder choice(@NotNull DialogueChoice choice) {
            this.choices.add(requireNonNull(choice, "choice"));
            return this;
        }

        @NotNull
        public Builder condition(@NotNull Predicate<DialogueContext> condition) {
            this.condition = requireNonNull(condition, "condition");
            return this;
        }

        @NotNull
        public Builder action(@NotNull DialogueAction action) {
            this.actions.add(requireNonNull(action, "action"));
            return this;
        }

        @NotNull
        public DialogueNode build() {
            return new DialogueNode(this);
        }
    }
}
