package dev.willram.ramcore.npc;

import org.jetbrains.annotations.NotNull;

/**
 * Handles player interactions with a managed NPC.
 */
@FunctionalInterface
public interface NpcClickHandler {

    void click(@NotNull NpcClickContext context);
}
