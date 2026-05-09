package dev.willram.ramcore.packet;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PacketVisualUtilitiesTest {

    @Test
    public void visualSessionTracksAndSendsViewerScopedOperations() {
        PacketViewer viewer = new PacketViewer(UUID.randomUUID(), "Will");
        InMemoryPacketVisualTransport transport = Packets.memoryTransport();
        PacketVisualSession session = Packets.session(viewer, transport);

        session.metadataPreview(42, "pose", "sleeping");
        session.glowing(42, true);
        session.equipment(42, EquipmentSlot.HAND, "diamond_sword");

        assertEquals(Optional.of("sleeping"), session.state().metadata(42, "pose"));
        assertTrue(session.state().glowing(42));
        assertEquals(Optional.of("diamond_sword"), session.state().equipment(42, EquipmentSlot.HAND));
        assertEquals(3, transport.sent().size());
        assertEquals(PacketVisualAction.METADATA_PREVIEW, transport.sent().getFirst().operation().action());
        assertEquals(viewer, transport.sent().getFirst().viewer());
    }

    @Test
    public void fakeEntityLifecycleIsPacketOnlyState() {
        PacketVisualSession session = Packets.session(new PacketViewer(UUID.randomUUID(), "Viewer"), Packets.memoryTransport());
        PacketFakeEntity fake = PacketFakeEntity.builder(9001, UUID.randomUUID(), "minecraft:armor_stand", "world")
                .position(1.0, 64.0, 2.0)
                .rotation(90.0f, 0.0f)
                .metadata("marker", true)
                .build();

        session.spawnFake(fake);

        assertEquals(Optional.of(fake), session.state().fakeEntity(9001));

        session.destroyFake(9001);

        assertTrue(session.state().fakeEntity(9001).isEmpty());
    }

    @Test
    public void resetClearsAllViewerVisualState() {
        PacketVisualSession session = Packets.session(new PacketViewer(UUID.randomUUID(), "Viewer"), Packets.memoryTransport());

        session.metadataPreview(7, "name", "Preview");
        session.glowing(7, true);
        session.equipment(7, EquipmentSlot.HEAD, "helmet");
        session.reset();

        assertTrue(session.state().metadata(7, "name").isEmpty());
        assertFalse(session.state().glowing(7));
        assertTrue(session.state().equipment(7, EquipmentSlot.HEAD).isEmpty());
    }

    @Test
    public void diagnosticsReflectProtocolAvailability() {
        PacketDiagnostics unavailable = Packets.diagnostics(false);
        PacketDiagnostics available = Packets.diagnostics(true);

        assertTrue(unavailable.supports(PacketFeature.PER_PLAYER_VISUAL_STATE));
        assertFalse(unavailable.supports(PacketFeature.FAKE_ENTITY));
        assertTrue(unavailable.warnings().stream().anyMatch(warning -> warning.contains("ProtocolLib")));
        assertTrue(available.supports(PacketFeature.FAKE_ENTITY));
        assertTrue(available.warnings().stream().anyMatch(warning -> warning.contains("version guarded")));
    }

    @Test
    public void packetCapabilityReportsProtocolAvailability() {
        NmsAccessRegistry available = Packets.registerProtocolCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE),
                true
        );
        NmsAccessRegistry unavailable = Packets.registerProtocolCapability(
                NmsAccessRegistry.create(MinecraftVersion.of(1, 21, 0), NmsVersion.NONE),
                false
        );

        assertEquals(NmsSupportStatus.PARTIAL, available.check(NmsCapability.PACKETS).status());
        assertEquals(NmsSupportStatus.UNSUPPORTED, unavailable.check(NmsCapability.PACKETS).status());
    }

    @Test
    public void scoreboardVisualOperationKeepsDataOutOfServerScoreboardState() {
        PacketVisualSession session = Packets.session(new PacketViewer(UUID.randomUUID(), "Viewer"), Packets.memoryTransport());

        PacketVisualOperation operation = session.scoreboardVisual("sidebar-preview", Map.of("line", "Wave 1"));

        assertEquals(PacketVisualAction.SCOREBOARD_VISUAL, operation.action());
        assertEquals("sidebar-preview", operation.data().get("id"));
        assertEquals("Wave 1", operation.data().get("line"));
    }
}
