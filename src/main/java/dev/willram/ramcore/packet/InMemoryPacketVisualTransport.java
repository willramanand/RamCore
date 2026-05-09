package dev.willram.ramcore.packet;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Packet transport useful for tests and diagnostics.
 */
public final class InMemoryPacketVisualTransport implements PacketVisualTransport {
    private final List<SentOperation> sent = new ArrayList<>();

    @Override
    public synchronized void send(@NotNull PacketViewer viewer, @NotNull PacketVisualOperation operation) {
        this.sent.add(new SentOperation(viewer, operation));
    }

    @NotNull
    public synchronized List<SentOperation> sent() {
        return List.copyOf(this.sent);
    }

    public synchronized void clear() {
        this.sent.clear();
    }

    public record SentOperation(@NotNull PacketViewer viewer, @NotNull PacketVisualOperation operation) {
        public SentOperation {
            Objects.requireNonNull(viewer, "viewer");
            Objects.requireNonNull(operation, "operation");
        }
    }
}
