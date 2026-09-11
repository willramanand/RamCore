package dev.willram.ramcore.packet;

import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * One viewer's packet-only visual session.
 */
public final class PacketVisualSession {
    private final PacketViewer viewer;
    private final PacketVisualTransport transport;
    private final PacketVisualState state = new PacketVisualState();

    PacketVisualSession(@NotNull PacketViewer viewer, @NotNull PacketVisualTransport transport) {
        this.viewer = Objects.requireNonNull(viewer, "viewer");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    @NotNull
    public PacketViewer viewer() {
        return this.viewer;
    }

    @NotNull
    public PacketVisualState state() {
        return this.state;
    }

    @NotNull
    public PacketVisualOperation metadataPreview(int entityId, @NotNull String key, @NotNull Object value) {
        return send(this.state.metadataPreview(entityId, key, value));
    }

    @NotNull
    public PacketVisualOperation clearMetadataPreview(int entityId) {
        return send(this.state.clearMetadataPreview(entityId));
    }

    @NotNull
    public PacketVisualOperation glowing(int entityId, boolean glowing) {
        return send(this.state.glowing(entityId, glowing));
    }

    @NotNull
    public PacketVisualOperation equipment(int entityId, @NotNull EquipmentSlot slot, @NotNull Object itemView) {
        return send(this.state.equipment(entityId, slot, itemView));
    }

    @NotNull
    public PacketVisualOperation clearEquipment(int entityId) {
        return send(this.state.clearEquipment(entityId));
    }

    @NotNull
    public PacketVisualOperation spawnFake(@NotNull PacketFakeEntity fakeEntity) {
        return send(this.state.spawnFake(fakeEntity));
    }

    @NotNull
    public PacketVisualOperation destroyFake(int entityId) {
        return send(this.state.destroyFake(entityId));
    }

    @NotNull
    public PacketVisualOperation scoreboardVisual(@NotNull String id, @NotNull Map<String, Object> data) {
        return send(this.state.scoreboardVisual(id, data));
    }

    @NotNull
    public PacketVisualOperation reset() {
        return send(this.state.reset());
    }

    @NotNull
    private PacketVisualOperation send(PacketVisualOperation operation) {
        this.transport.send(this.viewer, operation);
        return operation;
    }
}
