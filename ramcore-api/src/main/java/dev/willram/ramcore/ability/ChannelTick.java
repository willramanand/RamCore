package dev.willram.ramcore.ability;

import org.jetbrains.annotations.NotNull;

/**
 * A single tick of a channelled ability (see {@link AbilityChannel}).
 */
@FunctionalInterface
public interface ChannelTick {

    /**
     * Runs one channel tick.
     *
     * @param context the cast context (captured at channel start)
     * @param index   the 1-based tick index
     */
    void tick(@NotNull AbilityContext context, int index);
}
