package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * High-level owner for NMS-adjacent capability checks.
 */
public enum NmsCapabilityCategory {
    AI,
    BRAIN,
    PATHFINDING,
    ENTITY,
    COMBAT,
    ITEM,
    WORLD,
    LOOT,
    PACKET;

    @NotNull
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
