package dev.willram.ramcore.ability;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A combo: casting {@code steps} in order, each within {@code window} of the previous, triggers the
 * {@code finisher} ability. Tracked per player by {@link ComboTracker}.
 *
 * @param steps    the ordered ability ids that make up the combo (non-empty)
 * @param window   the maximum time span the whole combo may take
 * @param finisher the ability cast when the combo completes
 */
public record AbilityCombo(@NotNull List<ContentId> steps, @NotNull Duration window, @NotNull ContentId finisher) {

    public AbilityCombo {
        requireNonNull(window, "window");
        requireNonNull(finisher, "finisher");
        steps = List.copyOf(steps);
        RamPreconditions.checkArgument(!steps.isEmpty(), "combo steps must not be empty",
                "pass at least one step");
        RamPreconditions.checkArgument(!window.isNegative() && !window.isZero(),
                "combo window must be positive", "pass a positive window");
    }
}
