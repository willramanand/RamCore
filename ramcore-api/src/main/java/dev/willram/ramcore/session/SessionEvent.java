package dev.willram.ramcore.session;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * One recorded timeline event.
 *
 * @param tick   the server tick when it happened
 * @param at     wall-clock time
 * @param type   the event kind
 * @param detail a short free-form detail (redacted before display)
 */
public record SessionEvent(long tick, @NotNull Instant at, @NotNull SessionEventType type, @NotNull String detail) {

    public SessionEvent {
        requireNonNull(at, "at");
        requireNonNull(type, "type");
        requireNonNull(detail, "detail");
    }
}
