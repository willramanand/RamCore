package dev.willram.ramcore.trade;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered merchant trade set.
 */
public final class TradeProfile {
    private final List<TradeOffer> offers;
    private final TradeSetMode mode;
    private final TradeRestockBehavior restockBehavior;

    private TradeProfile(Builder builder) {
        this.offers = List.copyOf(builder.offers);
        this.mode = builder.mode;
        this.restockBehavior = builder.restockBehavior;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public static TradeProfile capture(@NotNull Merchant merchant) {
        Objects.requireNonNull(merchant, "merchant");
        Builder builder = builder();
        for (MerchantRecipe recipe : merchant.getRecipes()) {
            builder.offer(TradeOffer.capture(recipe));
        }
        return builder.build();
    }

    @NotNull
    public List<TradeOffer> offers() {
        return this.offers;
    }

    @NotNull
    public TradeSetMode mode() {
        return this.mode;
    }

    @NotNull
    public TradeRestockBehavior restockBehavior() {
        return this.restockBehavior;
    }

    @NotNull
    public List<MerchantRecipe> recipes() {
        return this.offers.stream().map(TradeOffer::toRecipe).toList();
    }

    public void apply(@NotNull Merchant merchant) {
        Objects.requireNonNull(merchant, "merchant");
        if (merchant instanceof AbstractVillager villager && this.restockBehavior == TradeRestockBehavior.RESET_OFFERS_BEFORE_APPLY) {
            villager.resetOffers();
        }

        List<MerchantRecipe> recipes = new ArrayList<>();
        if (this.mode == TradeSetMode.APPEND) {
            recipes.addAll(merchant.getRecipes());
        }
        recipes.addAll(recipes());
        merchant.setRecipes(recipes);

        if (merchant instanceof Villager villager) {
            if (this.restockBehavior == TradeRestockBehavior.RESTOCK_AFTER_APPLY) {
                villager.restock();
            } else if (this.restockBehavior == TradeRestockBehavior.UPDATE_DEMAND_AFTER_APPLY) {
                villager.updateDemand();
            }
        }
    }

    public static final class Builder {
        private final List<TradeOffer> offers = new ArrayList<>();
        private TradeSetMode mode = TradeSetMode.REPLACE;
        private TradeRestockBehavior restockBehavior = TradeRestockBehavior.NONE;

        private Builder() {
        }

        @NotNull
        public Builder offer(@NotNull TradeOffer offer) {
            this.offers.add(Objects.requireNonNull(offer, "offer"));
            return this;
        }

        @NotNull
        public Builder mode(@NotNull TradeSetMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        @NotNull
        public Builder restockBehavior(@NotNull TradeRestockBehavior restockBehavior) {
            this.restockBehavior = Objects.requireNonNull(restockBehavior, "restockBehavior");
            return this;
        }

        @NotNull
        public TradeProfile build() {
            return new TradeProfile(this);
        }
    }
}
