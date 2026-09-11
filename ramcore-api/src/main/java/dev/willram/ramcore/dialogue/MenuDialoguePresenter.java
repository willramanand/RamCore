package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.item.ItemStackBuilder;
import dev.willram.ramcore.menu.MenuView;
import dev.willram.ramcore.menu.Menus;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Presents a dialogue as a chest menu: one clickable item per choice, each wired to
 * {@link DialogueSession#choose(int)}. The menu grows to fit the choices (up to six rows).
 */
public final class MenuDialoguePresenter implements DialoguePresenter {
    private final String title;

    public MenuDialoguePresenter(@NotNull String title) {
        this.title = requireNonNull(title, "title");
    }

    @Override
    public void present(@NotNull DialogueSession session, @NotNull DialogueNode node,
                        @NotNull List<DialogueChoice> choices) {
        int rows = Math.max(1, Math.min(6, (choices.size() + 8) / 9));
        MenuView.Builder builder = Menus.menu(this.title, rows);
        for (int i = 0; i < choices.size() && i < rows * 9; i++) {
            int index = i;
            ItemStack icon = ItemStackBuilder.of(Material.PAPER).name(choices.get(i).label()).build();
            builder.button(i, Menus.button(icon)
                    .on(ClickType.LEFT, context -> session.choose(index))
                    .closeAfterClick(true)
                    .build());
        }
        Menus.open(session.player(), builder.build());
    }
}
