package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A dialogue: an identified graph of {@link DialogueNode}s with a start node. Validated on build so
 * the start node exists and every choice points at a real node (or ends the dialogue).
 */
public final class Dialogue {
    private final ContentId id;
    private final String startNodeId;
    private final Map<String, DialogueNode> nodes;

    private Dialogue(Builder builder) {
        this.id = requireNonNull(builder.id, "id");
        this.startNodeId = RamPreconditions.notBlank(builder.startNodeId, "startNodeId");
        this.nodes = Map.copyOf(builder.nodes);
        RamPreconditions.checkArgument(this.nodes.containsKey(this.startNodeId),
                "dialogue '" + this.id + "' start node '" + this.startNodeId + "' does not exist",
                "add a node with that id or set a valid start");
        for (DialogueNode node : this.nodes.values()) {
            for (DialogueChoice choice : node.choices()) {
                if (!choice.ends() && !this.nodes.containsKey(choice.nextNodeId())) {
                    throw RamPreconditions.misuse(
                            "dialogue '" + this.id + "' node '" + node.id() + "' choice points at missing node '"
                                    + choice.nextNodeId() + "'",
                            "add the target node or set nextNodeId to null to end the dialogue");
                }
            }
        }
    }

    @NotNull
    public static Builder builder(@NotNull ContentId id) {
        return new Builder(id);
    }

    @NotNull
    public ContentId id() {
        return this.id;
    }

    @NotNull
    public DialogueNode start() {
        return this.nodes.get(this.startNodeId);
    }

    @NotNull
    public Optional<DialogueNode> node(@NotNull String nodeId) {
        return Optional.ofNullable(this.nodes.get(nodeId));
    }

    @NotNull
    public Map<String, DialogueNode> nodes() {
        return this.nodes;
    }

    public static final class Builder {
        private final ContentId id;
        private String startNodeId;
        private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();

        private Builder(@NotNull ContentId id) {
            this.id = requireNonNull(id, "id");
        }

        /** Adds a node; the first node added becomes the start unless {@link #start(String)} is set. */
        @NotNull
        public Builder node(@NotNull DialogueNode node) {
            requireNonNull(node, "node");
            RamPreconditions.checkArgument(!this.nodes.containsKey(node.id()),
                    "duplicate dialogue node id '" + node.id() + "'", "use a unique node id");
            if (this.startNodeId == null) {
                this.startNodeId = node.id();
            }
            this.nodes.put(node.id(), node);
            return this;
        }

        @NotNull
        public Builder start(@NotNull String startNodeId) {
            this.startNodeId = requireNonNull(startNodeId, "startNodeId");
            return this;
        }

        @NotNull
        public Dialogue build() {
            return new Dialogue(this);
        }
    }
}
