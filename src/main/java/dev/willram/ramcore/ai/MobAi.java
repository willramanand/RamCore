package dev.willram.ramcore.ai;

import com.destroystokyo.paper.entity.ai.GoalKey;
import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Facade for Paper-backed mob AI helpers.
 */
public final class MobAi {

    @NotNull
    public static MobGoalBackend paperBackend() {
        return PaperMobGoalBackend.runtime();
    }

    @NotNull
    public static MobGoalBackend memoryBackend() {
        return new InMemoryMobGoalBackend();
    }

    @NotNull
    public static <T extends Mob> MobAiController<T> controller(@NotNull T mob) {
        return controller(mob, paperBackend());
    }

    @NotNull
    public static <T extends Mob> MobAiController<T> controller(@NotNull T mob, @NotNull MobGoalBackend backend) {
        return new MobAiController<>(mob, backend);
    }

    @NotNull
    public static <T extends Mob> GoalKey<T> key(@NotNull Class<T> mobType, @NotNull NamespacedKey key) {
        return GoalKey.of(mobType, key);
    }

    @NotNull
    public static <T extends Mob> RamMobGoalBuilder<T> goal(@NotNull GoalKey<T> key) {
        return new RamMobGoalBuilder<>(key);
    }

    @NotNull
    public static NmsCapabilityCheck paperCapability() {
        return NmsCapabilityCheck.supported(
                NmsCapability.MOB_GOALS,
                NmsAccessTier.PAPER_API,
                "paper-mob-goals",
                "Paper exposes MobGoals for add, remove, query, and running-goal inspection."
        );
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        return registry.override(paperCapability())
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.MOB_GOAL_SNAPSHOTS,
                        NmsAccessTier.PAPER_API,
                        "paper-mob-goals",
                        "RamCore can snapshot and restore goals it tracks; Paper does not expose complete vanilla priority snapshots."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.MOB_GOAL_DIAGNOSTICS,
                        NmsAccessTier.PAPER_API,
                        "paper-mob-goals",
                        "Paper exposes registered and running goals; RamCore augments diagnostics for tracked goals and conflicts."
                ));
    }

    private MobAi() {
    }
}
