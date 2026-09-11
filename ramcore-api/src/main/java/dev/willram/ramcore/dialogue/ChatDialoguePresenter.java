package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.text.Texts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Presents a dialogue in chat: the node text followed by one clickable line per choice, each wired to
 * {@link DialogueSession#choose(int)} via a Paper {@link ClickEvent#callback} — no command
 * registration.
 */
public final class ChatDialoguePresenter implements DialoguePresenter {

    @Override
    public void present(@NotNull DialogueSession session, @NotNull DialogueNode node,
                        @NotNull List<DialogueChoice> choices) {
        Player player = session.player();
        int generation = session.generation();
        player.sendMessage(Texts.render(node.text()));
        for (int i = 0; i < choices.size(); i++) {
            int index = i;
            Component line = Component.text()
                    .append(Component.text((i + 1) + ". "))
                    .append(Texts.render(choices.get(i).label()))
                    // guard stale clicks: an old chat line or a re-click after the dialogue advanced
                    .clickEvent(ClickEvent.callback(audience -> session.chooseIfCurrent(index, generation)))
                    .build();
            player.sendMessage(line);
        }
    }
}
