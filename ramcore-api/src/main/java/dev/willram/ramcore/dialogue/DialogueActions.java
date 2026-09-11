package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.menu.MenuView;
import dev.willram.ramcore.menu.Menus;
import dev.willram.ramcore.reward.RewardContext;
import dev.willram.ramcore.reward.RewardEngine;
import dev.willram.ramcore.reward.RewardPlan;
import dev.willram.ramcore.text.Texts;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Built-in {@link DialogueAction}s.
 */
public final class DialogueActions {
    private static final RewardEngine REWARD_ENGINE = new RewardEngine();

    private DialogueActions() {
    }

    /** Sends a MiniMessage line to the player. */
    @NotNull
    public static DialogueAction message(@NotNull String miniMessage) {
        requireNonNull(miniMessage, "miniMessage");
        return context -> context.player().sendMessage(Texts.render(miniMessage));
    }

    /** Makes the player run a command (without a leading slash). */
    @NotNull
    public static DialogueAction playerCommand(@NotNull String command) {
        requireNonNull(command, "command");
        return context -> context.player().performCommand(command);
    }

    /** Executes a reward plan for the player, scoped {@code "dialogue"}. */
    @NotNull
    public static DialogueAction reward(@NotNull RewardPlan plan) {
        return reward(REWARD_ENGINE, plan);
    }

    /** Executes a reward plan with a specific engine. */
    @NotNull
    public static DialogueAction reward(@NotNull RewardEngine engine, @NotNull RewardPlan plan) {
        requireNonNull(engine, "engine");
        requireNonNull(plan, "plan");
        return context -> engine.execute(plan,
                RewardContext.of("dialogue").withSubject(context.player()),
                ThreadLocalRandom.current());
    }

    /** Opens a menu for the player. */
    @NotNull
    public static DialogueAction openMenu(@NotNull MenuView view) {
        requireNonNull(view, "view");
        return context -> Menus.open(context.player(), view);
    }

    /** An arbitrary action. */
    @NotNull
    public static DialogueAction custom(@NotNull Consumer<DialogueContext> action) {
        requireNonNull(action, "action");
        return action::accept;
    }
}
