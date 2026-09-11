package dev.willram.ramcore.content.spec;

import dev.willram.ramcore.content.ContentDeserializeException;
import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.dialogue.Dialogue;
import dev.willram.ramcore.dialogue.DialogueAction;
import dev.willram.ramcore.dialogue.DialogueActions;
import dev.willram.ramcore.dialogue.DialogueChoice;
import dev.willram.ramcore.dialogue.DialogueConditions;
import dev.willram.ramcore.dialogue.DialogueNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * A pure description of a {@link Dialogue}. Config type {@code dialogues}. Covers the data-expressible
 * subset: node text, choices, {@code permission} conditions, and {@code messages}/{@code commands}
 * actions. Reward/menu/custom actions are code-supplied — build such dialogues programmatically.
 *
 * @param start the start node id
 * @param nodes the node specs
 */
public record DialogueSpec(@NotNull String start, @NotNull List<NodeSpec> nodes) {

    /** A dialogue node in config. */
    public record NodeSpec(@NotNull String id, @NotNull String text, @NotNull List<String> messages,
                           @NotNull List<String> commands, @Nullable String permission,
                           @NotNull List<ChoiceSpec> choices) {
    }

    /** A dialogue choice in config. */
    public record ChoiceSpec(@NotNull String label, @Nullable String next, @Nullable String permission,
                             @NotNull List<String> messages, @NotNull List<String> commands) {
    }

    public DialogueSpec {
        requireNonNull(start, "start");
        nodes = List.copyOf(nodes);
    }

    @NotNull
    public static DialogueSpec deserialize(@NotNull ConfigurationNode node) {
        String start = node.node("start").getString();
        List<NodeSpec> nodes = new ArrayList<>();
        node.node("nodes").childrenMap().forEach((key, value) -> nodes.add(nodeSpec(String.valueOf(key), value)));
        if (nodes.isEmpty()) {
            throw new ContentDeserializeException("dialogue has no nodes");
        }
        if (start == null || start.isBlank()) {
            start = nodes.get(0).id();
        }
        return new DialogueSpec(start, nodes);
    }

    private static NodeSpec nodeSpec(@NotNull String id, @NotNull ConfigurationNode node) {
        String text = node.node("text").getString("");
        List<ChoiceSpec> choices = new ArrayList<>();
        for (ConfigurationNode choice : node.node("choices").childrenList()) {
            choices.add(choiceSpec(choice));
        }
        return new NodeSpec(id, text, strings(node.node("messages")), strings(node.node("commands")),
                node.node("permission").getString(), choices);
    }

    private static ChoiceSpec choiceSpec(@NotNull ConfigurationNode node) {
        String label = node.node("label").getString();
        if (label == null || label.isBlank()) {
            throw new ContentDeserializeException("dialogue choice is missing 'label'");
        }
        return new ChoiceSpec(label, node.node("next").getString(), node.node("permission").getString(),
                strings(node.node("messages")), strings(node.node("commands")));
    }

    private static List<String> strings(@NotNull ConfigurationNode node) {
        List<String> out = new ArrayList<>();
        for (ConfigurationNode child : node.childrenList()) {
            String value = child.getString();
            if (value != null) {
                out.add(value);
            }
        }
        return out;
    }

    /**
     * Builds the {@link Dialogue}. Range/target validity is enforced by the {@code Dialogue}
     * constructor.
     *
     * @param id the dialogue id
     * @return the dialogue
     */
    @NotNull
    public Dialogue toDialogue(@NotNull ContentId id) {
        Dialogue.Builder builder = Dialogue.builder(id).start(this.start);
        for (NodeSpec nodeSpec : this.nodes) {
            DialogueNode.Builder nodeBuilder = DialogueNode.builder(nodeSpec.id(), nodeSpec.text());
            if (nodeSpec.permission() != null) {
                nodeBuilder.condition(DialogueConditions.permission(nodeSpec.permission()));
            }
            for (DialogueAction action : actions(nodeSpec.messages(), nodeSpec.commands())) {
                nodeBuilder.action(action);
            }
            for (ChoiceSpec choiceSpec : nodeSpec.choices()) {
                Predicate<dev.willram.ramcore.dialogue.DialogueContext> condition = choiceSpec.permission() == null
                        ? DialogueConditions.always()
                        : DialogueConditions.permission(choiceSpec.permission());
                nodeBuilder.choice(new DialogueChoice(choiceSpec.label(), choiceSpec.next(), condition,
                        actions(choiceSpec.messages(), choiceSpec.commands())));
            }
            builder.node(nodeBuilder.build());
        }
        return builder.build();
    }

    private static List<DialogueAction> actions(@NotNull List<String> messages, @NotNull List<String> commands) {
        List<DialogueAction> actions = new ArrayList<>();
        messages.forEach(message -> actions.add(DialogueActions.message(message)));
        commands.forEach(command -> actions.add(DialogueActions.playerCommand(command)));
        return actions;
    }
}
