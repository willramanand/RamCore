package dev.willram.ramcore.trade;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Facade for merchant and villager trade utilities.
 */
public final class Trades {

    @NotNull
    public static TradeOffer.Builder offer(@NotNull ItemStack result) {
        return TradeOffer.builder(result);
    }

    @NotNull
    public static TradeProfile.Builder profile() {
        return TradeProfile.builder();
    }

    public static void applyNow(@NotNull Merchant merchant, @NotNull TradeProfile profile) {
        Objects.requireNonNull(profile, "profile").apply(Objects.requireNonNull(merchant, "merchant"));
    }

    @NotNull
    public static Promise<Void> apply(@NotNull AbstractVillager villager, @NotNull TradeProfile profile) {
        Objects.requireNonNull(villager, "villager");
        Objects.requireNonNull(profile, "profile");
        return Schedulers.run(villager, () -> profile.apply(villager));
    }

    private Trades() {
    }
}
