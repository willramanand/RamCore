package dev.willram.ramcore.economy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class InMemoryEconomyTest {
    private final Economy economy = new InMemoryEconomy();
    private final UUID player = UUID.randomUUID();

    @Test
    public void depositIncreasesBalance() {
        EconomyResult result = this.economy.deposit(this.player, 100);
        assertTrue(result.success());
        assertEquals(100, result.newBalance());
        assertEquals(100, this.economy.balance(this.player));
    }

    @Test
    public void withdrawSucceedsWhenAffordable() {
        this.economy.deposit(this.player, 100);
        EconomyResult result = this.economy.withdraw(this.player, 30);
        assertTrue(result.success());
        assertEquals(70, result.newBalance());
        assertTrue(this.economy.has(this.player, 70));
        assertFalse(this.economy.has(this.player, 71));
    }

    @Test
    public void withdrawFailsWithoutFundsAndLeavesBalance() {
        this.economy.deposit(this.player, 10);
        EconomyResult result = this.economy.withdraw(this.player, 50);
        assertFalse(result.success());
        assertEquals(10, result.newBalance());
        assertEquals("insufficient funds", result.message());
        assertEquals(10, this.economy.balance(this.player));
    }

    @Test
    public void unknownPlayerHasZeroBalance() {
        assertEquals(0, this.economy.balance(UUID.randomUUID()));
    }

    @Test
    public void negativeAmountsAreRejected() {
        assertThrows(dev.willram.ramcore.exception.ApiMisuseException.class, () -> this.economy.deposit(this.player, -1));
        assertThrows(dev.willram.ramcore.exception.ApiMisuseException.class, () -> this.economy.withdraw(this.player, -1));
    }

    @Test
    public void formatUsesCurrencyNames() {
        Economy named = new InMemoryEconomy("dollar", "dollars");
        assertEquals("1.00 dollar", named.format(1));
        assertEquals("2.50 dollars", named.format(2.5));
    }
}
