package dev.willram.ramcore.session;

/**
 * The kind of event recorded on a player's session timeline.
 */
public enum SessionEventType {
    REWARD,
    LOOT,
    OBJECTIVE,
    COOLDOWN_DENIED,
    REGION_ENTER,
    REGION_EXIT,
    MENU_OPEN,
    ABILITY_CAST,
    CUSTOM
}
