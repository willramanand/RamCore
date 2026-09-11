package dev.willram.ramcore.trade;

/**
 * Optional villager-side action around recipe application.
 */
public enum TradeRestockBehavior {
    NONE,
    RESET_OFFERS_BEFORE_APPLY,
    RESTOCK_AFTER_APPLY,
    UPDATE_DEMAND_AFTER_APPLY
}
