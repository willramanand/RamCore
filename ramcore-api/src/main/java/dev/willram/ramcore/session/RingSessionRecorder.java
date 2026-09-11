package dev.willram.ramcore.session;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;

/**
 * A {@link SessionRecorder} backed by one fixed-size ring buffer per player. Appends allocate no map
 * entries on the hot path after the player's buffer exists.
 */
final class RingSessionRecorder implements SessionRecorder {
    private final int capacity;
    private final Clock clock;
    private final LongSupplier tick;
    private final Map<UUID, Ring> buffers = new ConcurrentHashMap<>();

    RingSessionRecorder(int capacity, @NotNull Clock clock, @NotNull LongSupplier tick) {
        RamPreconditions.checkArgument(capacity > 0, "session buffer capacity must be > 0", "Use a positive capacity such as 64.");
        this.capacity = capacity;
        this.clock = requireNonNull(clock, "clock");
        this.tick = requireNonNull(tick, "tick");
    }

    @Override
    public void record(@NotNull UUID player, @NotNull SessionEventType type, @NotNull String detail) {
        requireNonNull(player, "player");
        requireNonNull(type, "type");
        requireNonNull(detail, "detail");
        Ring ring = this.buffers.computeIfAbsent(player, id -> new Ring(this.capacity));
        ring.add(new SessionEvent(this.tick.getAsLong(), this.clock.instant(), type, detail));
    }

    @NotNull
    @Override
    public List<SessionEvent> timeline(@NotNull UUID player, int limit) {
        requireNonNull(player, "player");
        Ring ring = this.buffers.get(player);
        if (ring == null || limit <= 0) {
            return List.of();
        }
        return ring.snapshot(limit);
    }

    @Override
    public void clear(@NotNull UUID player) {
        this.buffers.remove(requireNonNull(player, "player"));
    }

    /** A single-player fixed ring buffer; all access synchronised on the instance. */
    private static final class Ring {
        private final SessionEvent[] events;
        private int size;
        private int head; // index of the next write

        Ring(int capacity) {
            this.events = new SessionEvent[capacity];
        }

        synchronized void add(SessionEvent event) {
            this.events[this.head] = event;
            this.head = (this.head + 1) % this.events.length;
            if (this.size < this.events.length) {
                this.size++;
            }
        }

        synchronized List<SessionEvent> snapshot(int limit) {
            int count = Math.min(limit, this.size);
            List<SessionEvent> result = new ArrayList<>(count);
            // oldest of the last `count` events, in chronological order
            int start = (this.head - count % this.events.length + this.events.length) % this.events.length;
            for (int i = 0; i < count; i++) {
                result.add(this.events[(start + i) % this.events.length]);
            }
            return result;
        }
    }
}
