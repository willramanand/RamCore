package dev.willram.ramcore.session;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Records a bounded, per-player timeline of gameplay events for diagnostics. The disabled instance
 * is {@link #NOOP}, so instrumentation call sites stay free when recording is off.
 *
 * <p>Stability: stable command contract. Folia-safe: per-player buffers are independent and
 * concurrent.</p>
 */
public interface SessionRecorder {

    /** A recorder that ignores everything and returns empty timelines. */
    SessionRecorder NOOP = new SessionRecorder() {
        @Override
        public void record(@NotNull UUID player, @NotNull SessionEventType type, @NotNull String detail) {
        }

        @NotNull
        @Override
        public List<SessionEvent> timeline(@NotNull UUID player, int limit) {
            return List.of();
        }

        @Override
        public void clear(@NotNull UUID player) {
        }

        @Override
        public boolean enabled() {
            return false;
        }
    };

    /**
     * A ring-buffer recorder holding up to {@code capacity} events per player.
     *
     * @param capacity events kept per player
     * @return the recorder
     */
    @NotNull
    static SessionRecorder ring(int capacity) {
        return new RingSessionRecorder(capacity, java.time.Clock.systemUTC(), () -> 0L);
    }

    /**
     * A ring-buffer recorder with an injected clock and tick source (for tests / server ticks).
     *
     * @param capacity events kept per player
     * @param clock    wall-clock source
     * @param tick     current-tick source
     * @return the recorder
     */
    @NotNull
    static SessionRecorder ring(int capacity, @NotNull java.time.Clock clock, @NotNull java.util.function.LongSupplier tick) {
        return new RingSessionRecorder(capacity, clock, tick);
    }

    void record(@NotNull UUID player, @NotNull SessionEventType type, @NotNull String detail);

    /**
     * The most recent events for a player, oldest first, at most {@code limit}.
     *
     * @param player the player
     * @param limit  max events
     * @return the timeline
     */
    @NotNull
    List<SessionEvent> timeline(@NotNull UUID player, int limit);

    void clear(@NotNull UUID player);

    default boolean enabled() {
        return true;
    }
}
