package dev.willram.ramcore.menu;

import org.jetbrains.annotations.NotNull;

/**
 * Creates a button for one paginated entry.
 */
@FunctionalInterface
public interface PaginatedButtonFactory<T> {
    @NotNull
    MenuButton button(@NotNull T entry, int index);
}
