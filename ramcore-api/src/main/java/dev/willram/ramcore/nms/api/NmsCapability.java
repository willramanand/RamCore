package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NMS-adjacent capability areas exposed behind RamCore interfaces.
 */
public enum NmsCapability {
    MOB_GOALS(NmsCapabilityCategory.AI, "Paper-backed mob goal add/remove/query operations."),
    MOB_GOAL_SNAPSHOTS(NmsCapabilityCategory.AI, "RamCore-tracked goal snapshots and restoration."),
    MOB_GOAL_DIAGNOSTICS(NmsCapabilityCategory.AI, "Goal diagnostics for active, running, tracked, and conflicting goals."),

    BRAIN_MEMORY(NmsCapabilityCategory.BRAIN, "Broad brain memory access through public Paper memory keys."),
    BRAIN_MEMORY_READ(NmsCapabilityCategory.BRAIN, "Read selected LivingEntity memory keys."),
    BRAIN_MEMORY_WRITE(NmsCapabilityCategory.BRAIN, "Write and clear selected LivingEntity memory keys."),
    BRAIN_SENSORS(NmsCapabilityCategory.BRAIN, "Broad brain sensor inspection."),
    BRAIN_SENSOR_READ(NmsCapabilityCategory.BRAIN, "Read modern mob sensor state."),
    BRAIN_ACTIVITY(NmsCapabilityCategory.BRAIN, "Broad brain activity inspection or mutation."),
    BRAIN_ACTIVITY_READ(NmsCapabilityCategory.BRAIN, "Read current brain activities."),
    BRAIN_ACTIVITY_WRITE(NmsCapabilityCategory.BRAIN, "Force or clear brain activities."),

    PATHFINDING(NmsCapabilityCategory.PATHFINDING, "Broad pathfinding request and progress support."),
    PATHFINDING_ROUTES(NmsCapabilityCategory.PATHFINDING, "RamCore waypoint route and patrol controllers."),
    PATHFINDING_NAVIGATION_PROFILE(NmsCapabilityCategory.PATHFINDING, "Paper-exposed navigation toggles such as doors and floating."),
    PATHFINDING_NODE_CONTROL(NmsCapabilityCategory.PATHFINDING, "NMS node penalties and deeper navigation controls."),
    ENTITY_MOVEMENT_CONTROLLERS(NmsCapabilityCategory.PATHFINDING, "Raw look, move, jump, flying, and swimming controllers."),

    ENTITY_CONTROL(NmsCapabilityCategory.ENTITY, "Broad entity state and control helpers."),
    ENTITY_SPAWNING(NmsCapabilityCategory.ENTITY, "Configured entity spawning from templates."),
    ENTITY_TEMPLATES(NmsCapabilityCategory.ENTITY, "RamCore entity template application."),
    ENTITY_TEMPORARY_MODIFIERS(NmsCapabilityCategory.ENTITY, "Temporary entity mutations with snapshot restoration."),
    ENTITY_EQUIPMENT(NmsCapabilityCategory.ENTITY, "Equipment and drop chance configuration."),
    ENTITY_LOOK_CONTROL(NmsCapabilityCategory.ENTITY, "Entity facing/look behavior."),
    ENTITY_MOVE_CONTROL(NmsCapabilityCategory.ENTITY, "Raw NMS move-control internals."),
    ENTITY_JUMP_CONTROL(NmsCapabilityCategory.ENTITY, "Raw NMS jump-control internals."),

    ENTITY_ATTRIBUTES(NmsCapabilityCategory.COMBAT, "Broad entity attributes and combat behavior."),
    ATTRIBUTE_MODIFIERS(NmsCapabilityCategory.COMBAT, "Vanilla attribute base values and modifiers."),
    DAMAGE_APPLICATION(NmsCapabilityCategory.COMBAT, "Paper/Bukkit damage application helpers."),
    COMBAT_INVULNERABILITY_TICKS(NmsCapabilityCategory.COMBAT, "No-damage tick and post-hit invulnerability controls."),
    COMBAT_ATTACK_COOLDOWN(NmsCapabilityCategory.COMBAT, "Raw attack cooldown internals."),
    COMBAT_HURT_TIMERS(NmsCapabilityCategory.COMBAT, "Raw hurt timer and attack animation internals."),
    COMBAT_DAMAGE_ROUTING(NmsCapabilityCategory.COMBAT, "Version-specific custom damage routing."),

    ITEM_DATA_COMPONENTS(NmsCapabilityCategory.ITEM, "Broad Paper experimental item data component access."),
    ITEM_COMPONENT_PATCHES(NmsCapabilityCategory.ITEM, "RamCore component patch read, diff, copy, and reset helpers."),
    ITEM_COMPONENT_PROFILES(NmsCapabilityCategory.ITEM, "High-level component profiles for common item behavior."),
    ITEM_NBT(NmsCapabilityCategory.ITEM, "Broad item NBT, identity, snapshot, and serialization support."),
    ITEM_BINARY_SERIALIZATION(NmsCapabilityCategory.ITEM, "Bukkit/Paper binary item serialization."),
    ITEM_SNBT(NmsCapabilityCategory.ITEM, "Raw item SNBT import/export."),
    ITEM_DIFFS(NmsCapabilityCategory.ITEM, "RamCore item snapshot diffing."),
    CUSTOM_ITEM_IDENTITY(NmsCapabilityCategory.ITEM, "Namespaced custom item identity tracking."),

    BLOCK_ENTITY_NBT(NmsCapabilityCategory.WORLD, "Broad block entity snapshot and NBT support."),
    BLOCK_ENTITY_SNAPSHOTS(NmsCapabilityCategory.WORLD, "Typed block-state snapshots for block entities."),
    RAW_BLOCK_ENTITY_SNBT(NmsCapabilityCategory.WORLD, "Raw block-entity SNBT import/export."),
    STRUCTURE_SNAPSHOTS(NmsCapabilityCategory.WORLD, "Structure capture and restore helpers."),
    SPAWNER_CONFIGURATION(NmsCapabilityCategory.WORLD, "Paper spawner configuration and weighted spawn entries."),

    LOOT_TABLES(NmsCapabilityCategory.LOOT, "Broad loot and trade manipulation support."),
    RAMCORE_LOOT_GENERATION(NmsCapabilityCategory.LOOT, "RamCore side-effect-free loot table generation."),
    LOOT_INSTANCES(NmsCapabilityCategory.LOOT, "Claimable generated loot instances."),
    VANILLA_LOOT_TABLE_MUTATION(NmsCapabilityCategory.LOOT, "Direct vanilla datapack loot table mutation."),
    TRADE_RECIPES(NmsCapabilityCategory.LOOT, "Paper merchant recipe builders and application."),
    TRADE_RESTOCK_INTERNALS(NmsCapabilityCategory.LOOT, "Hidden villager restock and demand internals."),

    PACKETS(NmsCapabilityCategory.PACKET, "Broad packet visual support."),
    PACKET_VISUAL_STATE(NmsCapabilityCategory.PACKET, "RamCore viewer-scoped packet visual state planning."),
    PACKET_PROTOCOLLIB_TRANSPORT(NmsCapabilityCategory.PACKET, "ProtocolLib-backed packet send transport."),
    PACKET_VERSIONED_FACTORIES(NmsCapabilityCategory.PACKET, "Version-specific concrete packet factories.");

    private final NmsCapabilityCategory category;
    private final String description;

    NmsCapability(@NotNull NmsCapabilityCategory category, @NotNull String description) {
        this.category = category;
        this.description = description;
    }

    @NotNull
    public NmsCapabilityCategory category() {
        return this.category;
    }

    @NotNull
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @NotNull
    public String description() {
        return this.description;
    }
}
