package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Example module descriptors for manual compatibility testing.
 */
public final class NmsExampleModules {

    @NotNull
    public static List<NmsExampleModule> defaults() {
        return List.of(
                new NmsExampleModule("mob-goals", NmsCapability.MOB_GOALS,
                        "Register, snapshot, and remove a RamCore mob goal.",
                        "Spawn a mob, add a custom goal, confirm diagnostics show it, then restore the snapshot."),
                new NmsExampleModule("brain-memory", NmsCapability.BRAIN_MEMORY,
                        "Read/write common mob brain memories through the Paper-backed facade.",
                        "Assign home/meeting/job memories and confirm diagnostics can read them back."),
                new NmsExampleModule("pathfinding", NmsCapability.PATHFINDING,
                        "Run a managed path task and patrol route.",
                        "Start a route, cancel it, and verify path result/status transitions."),
                new NmsExampleModule("path-navigation-profile", NmsCapability.PATHFINDING_NAVIGATION_PROFILE,
                        "Apply Paper-exposed navigation toggles.",
                        "Toggle door/floating navigation and confirm unsupported node controls stay disabled."),
                new NmsExampleModule("entity-control", NmsCapability.ENTITY_CONTROL,
                        "Apply entity flags, attributes, equipment, and temporary modifiers.",
                        "Spawn a configured mob and confirm snapshot restore resets visible flags."),
                new NmsExampleModule("combat-controls", NmsCapability.DAMAGE_APPLICATION,
                        "Apply damage profiles, attribute modifiers, and invulnerability tick settings.",
                        "Damage a test mob and confirm attack cooldown/hurt timer internals remain capability-gated."),
                new NmsExampleModule("item-components", NmsCapability.ITEM_DATA_COMPONENTS,
                        "Read, diff, copy, and apply item data component patches.",
                        "Apply a profile to a test item and verify the serialized patch remains version-bound."),
                new NmsExampleModule("item-nbt", NmsCapability.ITEM_NBT,
                        "Serialize, diff, and identify custom items.",
                        "Round-trip binary serialization and verify SNBT remains unavailable unless an adapter is installed."),
                new NmsExampleModule("block-entity-nbt", NmsCapability.BLOCK_ENTITY_NBT,
                        "Snapshot signs, containers, spawners, skulls, banners, lecterns, and command blocks.",
                        "Capture typed properties and verify raw SNBT is adapter-gated."),
                new NmsExampleModule("structure-spawners", NmsCapability.STRUCTURE_SNAPSHOTS,
                        "Capture/restore a small structure and configure a spawner.",
                        "Restore a test region and verify raw block-entity SNBT remains unavailable without an adapter."),
                new NmsExampleModule("loot-tables", NmsCapability.LOOT_TABLES,
                        "Roll RamCore loot pools and build merchant recipes.",
                        "Generate a table with conditions/functions and apply a trade profile to a test villager."),
                new NmsExampleModule("loot-instances", NmsCapability.LOOT_INSTANCES,
                        "Register and claim generated loot instances.",
                        "Verify duplicate-claim policy, expiry sweep, and reroll behavior."),
                new NmsExampleModule("packets", NmsCapability.PACKETS,
                        "Preview per-player packet visuals through a ProtocolLib transport.",
                        "Send metadata/glowing/equipment/fake entity previews to one viewer and confirm server state is unchanged."),
                new NmsExampleModule("packet-factory-gating", NmsCapability.PACKET_VERSIONED_FACTORIES,
                        "Confirm concrete packet factories remain disabled without a versioned adapter.",
                        "Run packet diagnostics with and without ProtocolLib and verify factory capabilities stay unsupported.")
        );
    }

    private NmsExampleModules() {
    }
}
