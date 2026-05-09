package dev.willram.ramcore.trade;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class TradeUtilitiesTest {

    @Test
    public void tradeOfferBuildsMerchantRecipeWithPricingFields() {
        TradeOffer offer = Trades.offer(item())
                .ingredient(item())
                .maxUses(8)
                .uses(2)
                .experienceReward(false)
                .villagerExperience(4)
                .priceMultiplier(0.25f)
                .demand(3)
                .specialPrice(-1)
                .ignoreDiscounts(true)
                .build();

        MerchantRecipe recipe = offer.toRecipe();

        assertFalse(recipe.getResult().isEmpty());
        assertEquals(1, recipe.getIngredients().size());
        assertEquals(2, recipe.getUses());
        assertEquals(8, recipe.getMaxUses());
        assertFalse(recipe.hasExperienceReward());
        assertEquals(4, recipe.getVillagerExperience());
        assertEquals(0.25f, recipe.getPriceMultiplier(), 0.0001f);
        assertEquals(3, recipe.getDemand());
        assertEquals(-1, recipe.getSpecialPrice());
        assertTrue(recipe.shouldIgnoreDiscounts());
    }

    @Test
    public void tradeOfferRejectsMoreThanTwoIngredients() {
        TradeOffer.Builder builder = Trades.offer(item())
                .ingredient(item())
                .ingredient(item());

        assertThrows(IllegalArgumentException.class, () -> builder.ingredient(item()));
    }

    @Test
    public void tradeProfileCanAppendOrReplaceRecipes() {
        CapturingMerchant handler = new CapturingMerchant(List.of(new MerchantRecipe(item(), 1)));
        Merchant merchant = handler.proxy();
        TradeOffer diamond = Trades.offer(item())
                .ingredient(item())
                .build();

        Trades.applyNow(merchant, Trades.profile()
                .mode(TradeSetMode.APPEND)
                .offer(diamond)
                .build());

        assertEquals(2, handler.recipes.size());

        Trades.applyNow(merchant, Trades.profile()
                .mode(TradeSetMode.REPLACE)
                .offer(diamond)
                .build());

        assertEquals(1, handler.recipes.size());
        assertFalse(handler.recipes.getFirst().getResult().isEmpty());
    }

    private static ItemStack item() {
        return new TestItemStack();
    }

    private static final class TestItemStack extends ItemStack {
        private TestItemStack() {
            super();
        }

        @Override
        public ItemStack clone() {
            return new TestItemStack();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private static final class CapturingMerchant implements InvocationHandler {
        private final List<MerchantRecipe> recipes = new ArrayList<>();

        private CapturingMerchant(List<MerchantRecipe> recipes) {
            this.recipes.addAll(recipes);
        }

        private Merchant proxy() {
            return (Merchant) Proxy.newProxyInstance(
                    Merchant.class.getClassLoader(),
                    new Class<?>[]{Merchant.class},
                    this
            );
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "getRecipes" -> List.copyOf(this.recipes);
                case "setRecipes" -> {
                    this.recipes.clear();
                    this.recipes.addAll((List<MerchantRecipe>) args[0]);
                    yield null;
                }
                case "getRecipeCount" -> this.recipes.size();
                case "toString" -> "CapturingMerchant";
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
