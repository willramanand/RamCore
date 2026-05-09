package dev.willram.ramcore.trade;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable builder-backed MerchantRecipe specification.
 */
public final class TradeOffer {
    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private final int uses;
    private final int maxUses;
    private final boolean experienceReward;
    private final int villagerExperience;
    private final float priceMultiplier;
    private final int demand;
    private final int specialPrice;
    private final boolean ignoreDiscounts;

    private TradeOffer(Builder builder) {
        this.result = builder.result.clone();
        this.ingredients = builder.ingredients.stream().map(ItemStack::clone).toList();
        this.uses = builder.uses;
        this.maxUses = builder.maxUses;
        this.experienceReward = builder.experienceReward;
        this.villagerExperience = builder.villagerExperience;
        this.priceMultiplier = builder.priceMultiplier;
        this.demand = builder.demand;
        this.specialPrice = builder.specialPrice;
        this.ignoreDiscounts = builder.ignoreDiscounts;
        if (this.maxUses < 0) {
            throw new IllegalArgumentException("maxUses cannot be negative");
        }
        if (this.uses < 0) {
            throw new IllegalArgumentException("uses cannot be negative");
        }
        if (this.uses > this.maxUses && this.maxUses > 0) {
            throw new IllegalArgumentException("uses cannot exceed maxUses");
        }
        if (this.ingredients.size() > 2) {
            throw new IllegalArgumentException("merchant recipes support at most two ingredients");
        }
        if (this.villagerExperience < 0) {
            throw new IllegalArgumentException("villagerExperience cannot be negative");
        }
        if (this.priceMultiplier < 0.0f) {
            throw new IllegalArgumentException("priceMultiplier cannot be negative");
        }
    }

    @NotNull
    public static Builder builder(@NotNull ItemStack result) {
        return new Builder(result);
    }

    @NotNull
    public static TradeOffer capture(@NotNull MerchantRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        Builder builder = builder(recipe.getResult())
                .uses(recipe.getUses())
                .maxUses(recipe.getMaxUses())
                .experienceReward(recipe.hasExperienceReward())
                .villagerExperience(recipe.getVillagerExperience())
                .priceMultiplier(recipe.getPriceMultiplier())
                .demand(recipe.getDemand())
                .specialPrice(recipe.getSpecialPrice())
                .ignoreDiscounts(recipe.shouldIgnoreDiscounts());
        for (ItemStack ingredient : recipe.getIngredients()) {
            builder.ingredient(ingredient);
        }
        return builder.build();
    }

    @NotNull
    public ItemStack result() {
        return this.result.clone();
    }

    @NotNull
    public List<ItemStack> ingredients() {
        return this.ingredients.stream().map(ItemStack::clone).toList();
    }

    public int uses() {
        return this.uses;
    }

    public int maxUses() {
        return this.maxUses;
    }

    public boolean experienceReward() {
        return this.experienceReward;
    }

    public int villagerExperience() {
        return this.villagerExperience;
    }

    public float priceMultiplier() {
        return this.priceMultiplier;
    }

    public int demand() {
        return this.demand;
    }

    public int specialPrice() {
        return this.specialPrice;
    }

    public boolean ignoreDiscounts() {
        return this.ignoreDiscounts;
    }

    @NotNull
    public MerchantRecipe toRecipe() {
        MerchantRecipe recipe = new MerchantRecipe(
                this.result.clone(),
                this.uses,
                this.maxUses,
                this.experienceReward,
                this.villagerExperience,
                this.priceMultiplier,
                this.demand,
                this.specialPrice,
                this.ignoreDiscounts
        );
        recipe.setIngredients(ingredients());
        return recipe;
    }

    public static final class Builder {
        private ItemStack result;
        private final List<ItemStack> ingredients = new ArrayList<>();
        private int uses;
        private int maxUses = 1;
        private boolean experienceReward = true;
        private int villagerExperience;
        private float priceMultiplier;
        private int demand;
        private int specialPrice;
        private boolean ignoreDiscounts;

        private Builder(ItemStack result) {
            this.result = Objects.requireNonNull(result, "result").clone();
        }

        @NotNull
        public Builder result(@NotNull ItemStack result) {
            this.result = Objects.requireNonNull(result, "result").clone();
            return this;
        }

        @NotNull
        public Builder customizeResult(@NotNull Consumer<ItemStack> customizer) {
            Objects.requireNonNull(customizer, "customizer").accept(this.result);
            return this;
        }

        @NotNull
        public Builder ingredient(@NotNull ItemStack ingredient) {
            if (this.ingredients.size() >= 2) {
                throw new IllegalArgumentException("merchant recipes support at most two ingredients");
            }
            this.ingredients.add(Objects.requireNonNull(ingredient, "ingredient").clone());
            return this;
        }

        @NotNull
        public Builder ingredients(@NotNull List<ItemStack> ingredients) {
            Objects.requireNonNull(ingredients, "ingredients");
            this.ingredients.clear();
            for (ItemStack ingredient : ingredients) {
                ingredient(ingredient);
            }
            return this;
        }

        @NotNull
        public Builder uses(int uses) {
            this.uses = uses;
            return this;
        }

        @NotNull
        public Builder maxUses(int maxUses) {
            this.maxUses = maxUses;
            return this;
        }

        @NotNull
        public Builder experienceReward(boolean experienceReward) {
            this.experienceReward = experienceReward;
            return this;
        }

        @NotNull
        public Builder villagerExperience(int villagerExperience) {
            this.villagerExperience = villagerExperience;
            return this;
        }

        @NotNull
        public Builder priceMultiplier(float priceMultiplier) {
            this.priceMultiplier = priceMultiplier;
            return this;
        }

        @NotNull
        public Builder demand(int demand) {
            this.demand = demand;
            return this;
        }

        @NotNull
        public Builder specialPrice(int specialPrice) {
            this.specialPrice = specialPrice;
            return this;
        }

        @NotNull
        public Builder ignoreDiscounts(boolean ignoreDiscounts) {
            this.ignoreDiscounts = ignoreDiscounts;
            return this;
        }

        @NotNull
        public TradeOffer build() {
            return new TradeOffer(this);
        }
    }
}
