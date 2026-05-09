package dev.willram.ramcore.menu;

import dev.willram.ramcore.text.TextContext;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Facade for inventory menu builders and sessions.
 */
public final class Menus {

    @NotNull
    public static MenuView.Builder menu(@NotNull ComponentLike title, int rows) {
        return MenuView.builder(title, rows);
    }

    @NotNull
    public static MenuView.Builder menu(@NotNull String title, int rows) {
        return MenuView.builder(title, rows);
    }

    @NotNull
    public static MenuView.Builder menu(@NotNull String title, @NotNull TextContext context, int rows) {
        return MenuView.builder(title, context, rows);
    }

    @NotNull
    public static MenuButton.Builder button(@NotNull ItemStack itemStack) {
        return MenuButton.builder(itemStack);
    }

    @NotNull
    public static MenuSession open(@NotNull Player player, @NotNull MenuView view) {
        return MenuSession.open(player, view);
    }

    @NotNull
    public static <T> PaginatedMenu.Builder<T> paginated(@NotNull ComponentLike title, int rows, @NotNull PaginatedButtonFactory<T> buttonFactory) {
        return PaginatedMenu.builder(title, rows, buttonFactory);
    }

    private Menus() {
    }
}
