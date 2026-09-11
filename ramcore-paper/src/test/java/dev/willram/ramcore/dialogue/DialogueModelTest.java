package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DialogueModelTest {
    private static final ContentId GREETER = ContentId.parse("test:greeter");

    private static Player player(boolean permitted) {
        return ProxyFakes.proxy(Player.class, Map.of(
                "getUniqueId", UUID.randomUUID(),
                "hasPermission", permitted));
    }

    @Test
    public void buildValidatesStartNode() {
        assertThrows(RuntimeException.class, () -> Dialogue.builder(GREETER)
                .node(DialogueNode.builder("a", "hi").build())
                .start("missing")
                .build());
    }

    @Test
    public void buildValidatesChoiceTargets() {
        assertThrows(RuntimeException.class, () -> Dialogue.builder(GREETER)
                .node(DialogueNode.builder("a", "hi").choice(DialogueChoice.of("go", "nowhere")).build())
                .build());
    }

    @Test
    public void firstNodeBecomesStart() {
        Dialogue dialogue = Dialogue.builder(GREETER)
                .node(DialogueNode.builder("root", "hello").choice(DialogueChoice.of("bye", null)).build())
                .build();
        assertEquals("root", dialogue.start().id());
        assertTrue(dialogue.node("root").isPresent());
    }

    @Test
    public void availableChoicesFilteredByCondition() {
        DialogueNode node = DialogueNode.builder("root", "pick")
                .choice(DialogueChoice.of("always", null))
                .choice(DialogueChoice.of("vip", null, DialogueConditions.metadataEquals("vip", true)))
                .build();

        Player player = player(true);
        assertEquals(1, node.availableChoices(DialogueContext.of(player)).size());
        assertEquals(2, node.availableChoices(DialogueContext.of(player).withMetadata(Map.of("vip", true))).size());
    }

    @Test
    public void permissionConditionUsesPlayer() {
        var condition = DialogueConditions.permission("ramcore.vip");
        assertTrue(condition.test(DialogueContext.of(player(true))));
        assertFalse(condition.test(DialogueContext.of(player(false))));
    }

    @Test
    public void customActionRuns() {
        AtomicInteger ran = new AtomicInteger();
        DialogueAction action = DialogueActions.custom(ctx -> ran.incrementAndGet());
        action.run(DialogueContext.of(player(true)));
        assertEquals(1, ran.get());
    }

    @Test
    public void registryLifecycle() {
        DialogueRegistry registry = new DialogueRegistry();
        Dialogue dialogue = Dialogue.builder(GREETER)
                .node(DialogueNode.builder("root", "hi").build()).build();
        registry.register("test", dialogue);

        assertTrue(registry.contains(GREETER));
        assertEquals(dialogue, registry.require(GREETER));
        assertThrows(RuntimeException.class, () -> registry.register("test", dialogue));
        assertEquals(1, registry.unregisterOwner("test"));
        assertFalse(registry.contains(GREETER));
    }
}
