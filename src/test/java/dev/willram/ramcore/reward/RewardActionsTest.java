package dev.willram.ramcore.reward;

import dev.willram.ramcore.economy.Economy;
import dev.willram.ramcore.economy.InMemoryEconomy;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RewardActionsTest {
    private final Economy economy = new InMemoryEconomy();
    private final RewardEngine engine = Rewards.engine();
    private final UUID player = UUID.randomUUID();

    @Test
    public void moneyRewardDepositsThroughTheEngine() {
        RewardPlan plan = Rewards.plan()
                .guaranteed(RewardEntry.guaranteed("money", RewardActions.money(this.economy, 250)))
                .build();

        RewardReport report = this.engine.execute(plan, RewardContext.of("quest").withSubject(this.player), new Random(1));

        assertTrue(report.successful());
        assertEquals(250, this.economy.balance(this.player));
        assertEquals(1, report.outcomes().size());
        assertTrue(report.outcomes().get(0).success());
    }

    @Test
    public void moneyRewardWithoutSubjectReportsValidationError() {
        RewardPlan plan = Rewards.plan()
                .guaranteed(RewardEntry.guaranteed("money", RewardActions.money(this.economy, 100)))
                .build();

        RewardReport report = this.engine.execute(plan, RewardContext.of("quest"), new Random(1));

        assertFalse(report.successful());
        assertTrue(report.errors().stream().anyMatch(e -> e.contains("player subject")));
        assertEquals(0, this.economy.balance(this.player));
    }

    @Test
    public void moneyResolvesUuidAndOfflinePlayerSubjects() {
        RewardContext byUuid = RewardContext.of("s").withSubject(this.player);
        assertTrue(RewardSubjects.playerId(byUuid).isPresent());
        assertEquals(this.player, RewardSubjects.playerId(byUuid).orElseThrow());
        assertTrue(RewardSubjects.playerId(RewardContext.of("s")).isEmpty());
    }

    @Test
    public void previewDoesNotTouchTheEconomy() {
        RewardPlan plan = Rewards.plan()
                .guaranteed(RewardEntry.guaranteed("money", RewardActions.money(this.economy, 500)))
                .build();

        RewardReport report = this.engine.preview(plan, RewardContext.of("quest").withSubject(this.player), new Random(1));

        assertTrue(report.successful());
        assertTrue(report.outcomes().get(0).preview());
        assertEquals(0, this.economy.balance(this.player), "preview must not deposit");
    }
}
