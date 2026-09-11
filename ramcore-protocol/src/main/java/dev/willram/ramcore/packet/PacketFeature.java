package dev.willram.ramcore.packet;

/**
 * Packet-only visual surfaces RamCore exposes as viewer-scoped state.
 */
public enum PacketFeature {
    SCOREBOARD_VISUALS(true),
    ENTITY_METADATA_PREVIEW(true),
    GLOWING_PREVIEW(true),
    FAKE_EQUIPMENT(true),
    FAKE_ENTITY(true),
    PER_PLAYER_VISUAL_STATE(false);

    private final boolean protocolLibBacked;

    PacketFeature(boolean protocolLibBacked) {
        this.protocolLibBacked = protocolLibBacked;
    }

    public boolean protocolLibBacked() {
        return this.protocolLibBacked;
    }
}
