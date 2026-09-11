package dev.willram.ramcore.ability;

/**
 * What caused an ability to be cast. Carried on {@link AbilityContext} so actions and effects can
 * vary by source.
 */
public enum AbilityTrigger {

    /** Right/left click with a tagged custom item ({@code PlayerInteractEvent}). */
    ITEM_USE,

    /** Selecting a bound hotbar slot ({@code PlayerItemHeldEvent}). */
    HOTBAR,

    /** A command. */
    COMMAND,

    /** Swapping items between hands ({@code PlayerSwapHandItemsEvent}). */
    SWAP_HANDS,

    /** Any consumer-defined trigger. */
    CUSTOM
}
