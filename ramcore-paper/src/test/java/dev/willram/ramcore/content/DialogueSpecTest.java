package dev.willram.ramcore.content;

import dev.willram.ramcore.content.spec.DialogueSpec;
import dev.willram.ramcore.dialogue.Dialogue;
import dev.willram.ramcore.dialogue.DialogueNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DialogueSpecTest {

    private SpecLoader loader() {
        return SpecLoader.create().deserializer("dialogues", DialogueSpec::deserialize);
    }

    private static void write(Path root, String name, String... lines) throws Exception {
        Path dir = root.resolve("dialogues");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    public void roundTripBuildsDialogue(@TempDir Path root) throws Exception {
        write(root, "quest.yml",
                "id: test:quest",
                "start: root",
                "nodes:",
                "  root:",
                "    text: '<green>Hello'",
                "    messages:",
                "      - '<gray>welcome'",
                "    choices:",
                "      - label: Accept",
                "        next: accept",
                "      - label: Leave",
                "  accept:",
                "    text: Thanks",
                "    permission: quest.accept");

        SpecLoadResult result = loader().load(root);
        assertTrue(result.successful(), () -> result.errors().toString());

        DialogueSpec spec = result.get(ContentId.parse("test:quest"), DialogueSpec.class).orElseThrow();
        assertEquals("root", spec.start());
        assertEquals(2, spec.nodes().size());

        Dialogue dialogue = spec.toDialogue(ContentId.parse("test:quest"));
        DialogueNode rootNode = dialogue.start();
        assertEquals("root", rootNode.id());
        assertEquals(2, rootNode.choices().size());
        assertTrue(rootNode.choices().get(1).ends()); // Leave has no next
        assertTrue(dialogue.node("accept").isPresent());
    }

    @Test
    public void badChoiceCollected(@TempDir Path root) throws Exception {
        write(root, "bad.yml",
                "id: test:bad",
                "nodes:",
                "  root:",
                "    text: hi",
                "    choices:",
                "      - next: root");

        SpecLoadResult result = loader().load(root);
        assertFalse(result.successful());
        assertTrue(result.errors().stream().anyMatch(e -> e.message().contains("missing 'label'")),
                () -> result.errors().toString());
    }
}
