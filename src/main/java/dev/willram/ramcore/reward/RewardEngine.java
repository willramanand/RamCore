package dev.willram.ramcore.reward;

import dev.willram.ramcore.random.RandomSelector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * Generic reward validation, preview, and execution pipeline.
 */
public final class RewardEngine {

    @NotNull
    public RewardReport validate(@NotNull RewardPlan plan, @NotNull RewardContext context) {
        return new RewardReport(false, List.of(), validateEntries(plan, context));
    }

    @NotNull
    public RewardReport preview(@NotNull RewardPlan plan, @NotNull RewardContext context, @NotNull Random random) {
        return run(plan, context, random, true);
    }

    @NotNull
    public RewardReport execute(@NotNull RewardPlan plan, @NotNull RewardContext context, @NotNull Random random) {
        return run(plan, context, random, false);
    }

    @NotNull
    private RewardReport run(@NotNull RewardPlan plan, @NotNull RewardContext context, @NotNull Random random, boolean preview) {
        requireNonNull(plan, "plan");
        requireNonNull(context, "context");
        requireNonNull(random, "random");

        List<String> errors = validateEntries(plan, context);
        if (!errors.isEmpty()) {
            return new RewardReport(preview, List.of(), errors);
        }

        List<RewardOutcome> outcomes = new ArrayList<>();
        for (RewardEntry entry : plan.guaranteed()) {
            if (entry.condition().test(context)) {
                outcomes.add(apply(entry, context, preview));
            }
        }

        List<RewardEntry> eligibleWeighted = plan.weighted().stream()
                .filter(entry -> entry.condition().test(context))
                .toList();
        if (!eligibleWeighted.isEmpty()) {
            RandomSelector<RewardEntry> selector = RandomSelector.weighted(eligibleWeighted);
            for (int i = 0; i < plan.rolls(); i++) {
                outcomes.add(apply(selector.pick(random), context, preview));
            }
        }

        return new RewardReport(preview, outcomes, List.of());
    }

    private static RewardOutcome apply(RewardEntry entry, RewardContext context, boolean preview) {
        return preview ? RewardOutcome.preview(entry.id()) : entry.action().apply(context);
    }

    private static List<String> validateEntries(RewardPlan plan, RewardContext context) {
        List<String> errors = new ArrayList<>();
        for (RewardEntry entry : plan.guaranteed()) {
            errors.addAll(entry.action().validate(context).stream()
                    .map(error -> entry.id() + ": " + error)
                    .toList());
        }
        for (RewardEntry entry : plan.weighted()) {
            errors.addAll(entry.action().validate(context).stream()
                    .map(error -> entry.id() + ": " + error)
                    .toList());
        }
        return errors;
    }
}
