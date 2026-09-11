package dev.willram.ramcore.menu;

import org.jetbrains.annotations.NotNull;

/**
 * Renders dynamic menu content for one session.
 */
@FunctionalInterface
public interface MenuRenderer {
    void render(@NotNull MenuSession session);
}
