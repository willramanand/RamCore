package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.testkit.ProxyFakes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DialogueSessionTest {
    private static final ContentId D = ContentId.parse("test:d");

    private static Player player() {
        return ProxyFakes.proxy(Player.class, Map.of("getUniqueId", UUID.randomUUID()));
    }

    private static final class Recorder implements DialoguePresenter {
        final List<String> presented = new ArrayList<>();

        @Override
        public void present(DialogueSession session, DialogueNode node, List<DialogueChoice> choices) {
            this.presented.add(node.id());
        }
    }

    @Test
    public void traversalFollowsChoices() {
        AtomicInteger choiceActionRuns = new AtomicInteger();
        Dialogue dialogue = Dialogue.builder(D)
                .node(DialogueNode.builder("root", "hi")
                        .choice(DialogueChoice.of("toA", "a").withActions(List.of(DialogueActions.custom(c -> choiceActionRuns.incrementAndGet()))))
                        .choice(DialogueChoice.of("bye", null))
                        .build())
                .node(DialogueNode.builder("a", "at a").choice(DialogueChoice.of("back", "root")).build())
                .build();

        Recorder recorder = new Recorder();
        DialogueSession session = DialogueSession.create(player(), dialogue, DialogueContext.of(player()), recorder);

        session.start();
        session.choose(0); // root -> a (runs choice action)
        session.choose(0); // a -> root
        session.choose(1); // root end

        assertEquals(List.of("root", "a", "root"), recorder.presented);
        assertEquals(1, choiceActionRuns.get());
        assertTrue(session.ended());
    }

    @Test
    public void nodeActionsRunOnEnter() {
        AtomicInteger entered = new AtomicInteger();
        Dialogue dialogue = Dialogue.builder(D)
                .node(DialogueNode.builder("root", "hi")
                        .action(DialogueActions.custom(c -> entered.incrementAndGet()))
                        .choice(DialogueChoice.of("bye", null))
                        .build())
                .build();
        DialogueSession session = DialogueSession.create(player(), dialogue, DialogueContext.of(player()), new Recorder());
        session.start();
        assertEquals(1, entered.get());
    }

    @Test
    public void nodeWithoutChoicesEnds() {
        Dialogue dialogue = Dialogue.builder(D)
                .node(DialogueNode.builder("root", "the end").build())
                .build();
        DialogueSession session = DialogueSession.create(player(), dialogue, DialogueContext.of(player()), new Recorder());
        session.start();
        assertTrue(session.ended());
        assertThrows(RuntimeException.class, () -> session.choose(0));
    }

    @Test
    public void choiceIndexValidated() {
        Dialogue dialogue = Dialogue.builder(D)
                .node(DialogueNode.builder("root", "hi").choice(DialogueChoice.of("bye", null)).build())
                .build();
        DialogueSession session = DialogueSession.create(player(), dialogue, DialogueContext.of(player()), new Recorder());
        session.start();
        assertThrows(RuntimeException.class, () -> session.choose(5));
        assertFalse(session.ended());
    }
}
