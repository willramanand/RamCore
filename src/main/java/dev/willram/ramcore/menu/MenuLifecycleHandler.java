package dev.willram.ramcore.menu;

import org.jetbrains.annotations.NotNull;

/**
 * Handles a menu session lifecycle signal.
 */
@FunctionalInterface
public interface MenuLifecycleHandler {
    void handle(@NotNull MenuSession session);
}
