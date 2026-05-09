package dev.willram.ramcore.menu;

import dev.willram.ramcore.exception.ApiMisuseException;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public final class MenuFrameworkTest {

    @Test
    public void menuViewStoresDeclarativeButtons() {
        MenuButton button = MenuButton.builder(item())
                .on(ClickType.LEFT, context -> {})
                .refreshAfterClick(true)
                .build();

        MenuView view = MenuView.builder(Component.text("Example"), 3)
                .button(13, button)
                .allowPlayerInventoryClicks(true)
                .updateEveryTicks(20L)
                .build();

        assertEquals(27, view.size());
        assertTrue(view.buttons().containsKey(13));
        assertTrue(view.playerInventoryClicksAllowed());
        assertEquals(20L, view.updateIntervalTicks());
    }

    @Test
    public void invalidSlotsFailFast() {
        try {
            MenuView.builder(Component.text("Example"), 1)
                    .button(9, null);
        } catch (ApiMisuseException e) {
            assertTrue(e.problem().contains("outside inventory bounds"));
            return;
        }

        throw new AssertionError("expected ApiMisuseException");
    }

    @Test
    public void menuStateStoresTypedViewerValues() {
        MenuState state = MenuState.create();
        state.put("page", 2);
        state.put("filter", "weapons");

        assertEquals(2, state.intValue("page", 0));
        assertEquals("weapons", state.get("filter", String.class).orElseThrow());
        assertFalse(state.get("filter", Integer.class).isPresent());
    }

    @Test
    public void itemStacksAreDefensivelyCopied() {
        ItemStack stack = item();
        MenuButton button = MenuButton.of(stack);
        ItemStack first = button.itemStack();
        ItemStack second = button.itemStack();

        assertNotSame(first, second);
    }

    @Test
    public void paginatedMenuCalculatesPagesAndNavigation() {
        MenuButton previous = MenuButton.of(item());
        MenuButton next = MenuButton.of(item());
        PaginatedMenu<String> menu = PaginatedMenu.<String>builder(Component.text("Pages"), 3, (entry, index) ->
                        MenuButton.of(item()))
                .entries(List.of("a", "b", "c", "d", "e"))
                .slots(0, 1)
                .previous(18, previous)
                .next(26, next)
                .build();

        assertEquals(3, menu.pageCount());
        assertEquals(List.of("c", "d"), menu.entriesForPage(1));

        Map<Integer, MenuButton> firstPage = menu.buttonsForPage(0);
        Map<Integer, MenuButton> middlePage = menu.buttonsForPage(1);
        Map<Integer, MenuButton> lastPage = menu.buttonsForPage(2);

        assertTrue(firstPage.containsKey(26));
        assertFalse(firstPage.containsKey(18));
        assertTrue(middlePage.containsKey(18));
        assertTrue(middlePage.containsKey(26));
        assertTrue(lastPage.containsKey(18));
        assertFalse(lastPage.containsKey(26));
    }

    private static ItemStack item() {
        return new FakeItemStack();
    }

    private static final class FakeItemStack extends ItemStack {
        private FakeItemStack() {
            super();
        }

        @Override
        public ItemStack clone() {
            return new FakeItemStack();
        }
    }
}
