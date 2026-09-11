package dev.willram.ramcore.ability;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A repeating channel for an ability: fires {@link ChannelTick} every {@code intervalTicks} for
 * {@code totalTicks}, then the ability's action executes as normal. An interrupted channel stops
 * early and does not execute the action. When set, the channel replaces the plain
 * {@link Ability#castTicks()} timer.
 *
 * @param intervalTicks ticks between channel ticks (positive)
 * @param totalTicks    total channel duration in ticks (positive, {@code >= intervalTicks})
 * @param onTick        the per-tick callback
 */
public record AbilityChannel(long intervalTicks, long totalTicks, @NotNull ChannelTick onTick) {

    public AbilityChannel {
        requireNonNull(onTick, "onTick");
        RamPreconditions.checkArgument(intervalTicks > 0L, "channel intervalTicks must be positive",
                "pass intervalTicks > 0");
        RamPreconditions.checkArgument(totalTicks >= intervalTicks,
                "channel totalTicks must be >= intervalTicks", "pass totalTicks >= intervalTicks");
    }

    /** The number of times {@link #onTick} fires over the channel. */
    public int tickCount() {
        return (int) (this.totalTicks / this.intervalTicks);
    }
}
