package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;

/**
 * Hook for party chat implementations.
 */
@FunctionalInterface
public interface PartyChatHandler {

    void chat(@NotNull PartyChatContext context);
}
