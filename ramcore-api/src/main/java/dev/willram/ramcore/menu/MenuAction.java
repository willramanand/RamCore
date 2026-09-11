package dev.willram.ramcore.menu;

import org.jetbrains.annotations.NotNull;

/**
 * Handles a menu button click.
 */
@FunctionalInterface
public interface MenuAction {
    void click(@NotNull MenuClickContext context);
}
