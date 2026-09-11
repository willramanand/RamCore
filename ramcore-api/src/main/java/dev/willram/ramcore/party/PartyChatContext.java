package dev.willram.ramcore.party;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Context for plugin-defined party chat handling.
 */
public record PartyChatContext(
        @NotNull PartyGroup party,
        @NotNull UUID sender,
        @NotNull String message
) {
}
