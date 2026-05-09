package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Current state for one player/resource-pack prompt pair.
 */
public record ResourcePackRequest(
        @NotNull UUID playerId,
        @NotNull UUID packId,
        @NotNull ResourcePackPrompt prompt,
        @NotNull ResourcePackPromptStatus status,
        long requestedAtMillis,
        long updatedAtMillis,
        long timeoutAtMillis
) {

    public boolean terminal() {
        return this.status.terminal();
    }

    public boolean timedOut(long nowMillis) {
        return !terminal() && this.timeoutAtMillis > 0L && nowMillis >= this.timeoutAtMillis;
    }

    @NotNull
    ResourcePackRequest status(@NotNull ResourcePackPromptStatus status, long nowMillis) {
        return new ResourcePackRequest(this.playerId, this.packId, this.prompt, status, this.requestedAtMillis, nowMillis, this.timeoutAtMillis);
    }
}
