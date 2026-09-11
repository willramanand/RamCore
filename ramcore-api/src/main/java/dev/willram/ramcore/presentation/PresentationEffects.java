package dev.willram.ramcore.presentation;

import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Built-in Adventure presentation effects.
 */
public final class PresentationEffects {

    @NotNull
    public static PresentationEffect noop() {
        return context -> Terminable.EMPTY;
    }

    @NotNull
    public static PresentationEffect message(@NotNull ComponentLike message) {
        requireNonNull(message, "message");
        return custom(audience -> audience.sendMessage(message));
    }

    @NotNull
    public static PresentationEffect actionBar(@NotNull ComponentLike message) {
        requireNonNull(message, "message");
        return custom(audience -> audience.sendActionBar(message));
    }

    @NotNull
    public static PresentationEffect title(@NotNull Title title) {
        requireNonNull(title, "title");
        return custom(audience -> audience.showTitle(title));
    }

    @NotNull
    public static PresentationEffect clearTitle() {
        return custom(Audience::clearTitle);
    }

    @NotNull
    public static PresentationEffect sound(@NotNull Sound sound) {
        requireNonNull(sound, "sound");
        return custom(audience -> audience.playSound(sound));
    }

    @NotNull
    public static PresentationEffect bossBar(@NotNull BossBar bossBar) {
        requireNonNull(bossBar, "bossBar");
        return context -> {
            context.forEachAudience(audience -> audience.showBossBar(bossBar));
            return () -> context.forEachAudience(audience -> audience.hideBossBar(bossBar));
        };
    }

    @NotNull
    public static PresentationEffect custom(@NotNull Consumer<Audience> action) {
        requireNonNull(action, "action");
        return context -> {
            context.forEachAudience(action);
            return Terminable.EMPTY;
        };
    }

    @NotNull
    public static PresentationSequence sequence() {
        return new PresentationSequence();
    }

    /**
     * Ordered effect sequence. Delayed steps use the context scheduler anchor.
     */
    public static final class PresentationSequence implements PresentationEffect {
        private final List<Step> steps = new ArrayList<>();

        @NotNull
        public PresentationSequence then(@NotNull PresentationEffect effect) {
            return then(effect, 0L);
        }

        @NotNull
        public PresentationSequence then(@NotNull PresentationEffect effect, long delayTicks) {
            this.steps.add(new Step(effect, delayTicks));
            return this;
        }

        @NotNull
        public PresentationSequence thenAll(@NotNull Collection<? extends PresentationEffect> effects) {
            requireNonNull(effects, "effects").forEach(this::then);
            return this;
        }

        @NotNull
        public List<Step> steps() {
            return List.copyOf(this.steps);
        }

        @NotNull
        @Override
        public Terminable play(@NotNull PresentationContext context) {
            CompositeTerminable terminables = CompositeTerminable.create();
            long offset = 0L;
            for (Step step : this.steps) {
                offset += Math.max(0L, step.delayTicks());
                if (offset == 0L) {
                    terminables.bind(step.effect().play(context));
                } else {
                    long delay = offset;
                    Schedulers.runLater(context.taskContext(), () -> terminables.bind(step.effect().play(context)), delay, terminables);
                }
            }
            return terminables;
        }
    }

    public record Step(@NotNull PresentationEffect effect, long delayTicks) {
        public Step {
            requireNonNull(effect, "effect");
        }
    }

    private PresentationEffects() {
    }
}
