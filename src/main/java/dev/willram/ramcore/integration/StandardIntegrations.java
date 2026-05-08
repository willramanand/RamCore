package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Built-in descriptors for common optional Minecraft plugin integrations.
 */
public final class StandardIntegrations {
    public static final IntegrationDescriptor LUCK_PERMS = descriptor(
            "luckperms",
            "LuckPerms",
            EnumSet.of(IntegrationCapability.PERMISSIONS, IntegrationCapability.GROUPS),
            "LuckPerms permission and group data"
    );

    public static final IntegrationDescriptor VAULT = descriptor(
            "vault",
            "Vault",
            EnumSet.of(IntegrationCapability.ECONOMY, IntegrationCapability.PERMISSIONS),
            "Vault economy and legacy permission bridge"
    );

    public static final IntegrationDescriptor PLACEHOLDER_API = descriptor(
            "placeholderapi",
            "PlaceholderAPI",
            EnumSet.of(IntegrationCapability.PLACEHOLDERS),
            "PlaceholderAPI placeholder resolution"
    );

    public static final IntegrationDescriptor MINI_PLACEHOLDERS = descriptor(
            "miniplaceholders",
            "MiniPlaceholders",
            EnumSet.of(IntegrationCapability.PLACEHOLDERS, IntegrationCapability.MINI_MESSAGE),
            "MiniPlaceholders MiniMessage placeholder resolution"
    );

    public static final IntegrationDescriptor WORLD_GUARD = descriptor(
            "worldguard",
            "WorldGuard",
            EnumSet.of(IntegrationCapability.REGIONS),
            "WorldGuard region capability"
    );

    public static final IntegrationDescriptor PROTOCOL_LIB = descriptor(
            "protocollib",
            "ProtocolLib",
            EnumSet.of(IntegrationCapability.PACKETS),
            "ProtocolLib packet capability"
    );

    public static final IntegrationDescriptor CITIZENS = descriptor(
            "citizens",
            "Citizens",
            EnumSet.of(IntegrationCapability.NPCS),
            "Citizens NPC capability"
    );

    public static final IntegrationDescriptor ITEMS_ADDER = descriptor(
            "itemsadder",
            "ItemsAdder",
            EnumSet.of(IntegrationCapability.CUSTOM_ITEMS, IntegrationCapability.RESOURCE_PACK_ITEMS),
            "ItemsAdder custom item and resource-pack item capability"
    );

    public static final IntegrationDescriptor ORAXEN = descriptor(
            "oraxen",
            "Oraxen",
            EnumSet.of(IntegrationCapability.CUSTOM_ITEMS, IntegrationCapability.RESOURCE_PACK_ITEMS),
            "Oraxen custom item and resource-pack item capability"
    );

    @NotNull
    public static List<IntegrationDescriptor> all() {
        return List.of(
                LUCK_PERMS,
                VAULT,
                PLACEHOLDER_API,
                MINI_PLACEHOLDERS,
                WORLD_GUARD,
                PROTOCOL_LIB,
                CITIZENS,
                ITEMS_ADDER,
                ORAXEN
        );
    }

    @NotNull
    private static IntegrationDescriptor descriptor(
            @NotNull String id,
            @NotNull String pluginName,
            @NotNull Set<IntegrationCapability> capabilities,
            @NotNull String description
    ) {
        return new IntegrationDescriptor(IntegrationId.of(id), pluginName, capabilities, description);
    }

    private StandardIntegrations() {
    }
}
