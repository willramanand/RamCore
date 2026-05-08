package dev.willram.ramcore.reward;

import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RewardEngineTest {

    @Test
    public void previewSelectsRewardsWithoutApplyingActions() {
        RewardEngine engine = new RewardEngine();
        AtomicInteger applied = new AtomicInteger();
        RewardPlan plan = RewardPlan.builder()
                .guaranteed(RewardEntry.guaranteed("message", context -> {
                    applied.incrementAndGet();
                    return RewardOutcome.success("message");
                }))
                .build();

        RewardReport report = engine.preview(plan, RewardContext.of("quest"), new Random(1));

        assertTrue(report.preview());
        assertEquals(1, report.outcomes().size());
        assertEquals("message", report.outcomes().getFirst().id());
        assertEquals(0, applied.get());
    }

    @Test
    public void executeAppliesGuaranteedAndWeightedRewards() {
        RewardEngine engine = new RewardEngine();
        AtomicInteger applied = new AtomicInteger();
        RewardAction action = context -> {
            applied.incrementAndGet();
            return RewardOutcome.success("reward");
        };
        RewardPlan plan = RewardPlan.builder()
                .guaranteed(RewardEntry.guaranteed("guaranteed", action))
                .weighted(RewardEntry.weighted("weighted", action, 1))
                .rolls(2)
                .build();

        RewardReport report = engine.execute(plan, RewardContext.of("boss"), new Random(1));

        assertTrue(report.successful());
        assertEquals(3, report.outcomes().size());
        assertEquals(3, applied.get());
    }

    @Test
    public void conditionSkipsIneligibleReward() {
        RewardEngine engine = new RewardEngine();
        AtomicInteger applied = new AtomicInteger();
        RewardPlan plan = RewardPlan.builder()
                .guaranteed(RewardEntry.guaranteed("locked", context -> {
                    applied.incrementAndGet();
                    return RewardOutcome.success("locked");
                }).when(context -> false))
                .build();

        RewardReport report = engine.execute(plan, RewardContext.of("crate"), new Random(1));

        assertTrue(report.successful());
        assertTrue(report.outcomes().isEmpty());
        assertEquals(0, applied.get());
    }

    @Test
    public void validationBlocksExecution() {
        RewardEngine engine = new RewardEngine();
        AtomicInteger applied = new AtomicInteger();
        RewardAction invalid = new RewardAction() {
            @Override
            public RewardOutcome apply(RewardContext context) {
                applied.incrementAndGet();
                return RewardOutcome.success("invalid");
            }

            @Override
            public List<String> validate(RewardContext context) {
                return List.of("missing item template");
            }
        };
        RewardPlan plan = RewardPlan.builder()
                .guaranteed(RewardEntry.guaranteed("item", invalid))
                .build();

        RewardReport report = engine.execute(plan, RewardContext.of("quest"), new Random(1));

        assertFalse(report.successful());
        assertEquals(List.of("item: missing item template"), report.errors());
        assertEquals(0, applied.get());
    }
}
