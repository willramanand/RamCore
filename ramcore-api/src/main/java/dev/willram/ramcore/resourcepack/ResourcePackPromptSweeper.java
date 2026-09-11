package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.scheduler.TaskContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;

/**
 * Drives {@link ResourcePackPromptTracker#sweepTimeouts(long)} on a repeating async timer so pending
 * prompts flip to {@code TIMED_OUT} without a caller polling. The tracker already knows how to sweep;
 * this is the missing scheduled invocation.
 */
public final class ResourcePackPromptSweeper {

    private ResourcePackPromptSweeper() {
    }

    /**
     * Starts a sweeper using the system clock.
     *
     * @param tracker       the tracker to sweep
     * @param intervalTicks how often to sweep, in ticks
     * @return the timer task; {@link Task#stop()} stops sweeping
     */
    @NotNull
    public static Task start(@NotNull ResourcePackPromptTracker tracker, long intervalTicks) {
        return start(tracker, intervalTicks, System::currentTimeMillis);
    }

    /**
     * Starts a sweeper with an injectable clock (tests).
     *
     * @param tracker       the tracker to sweep
     * @param intervalTicks how often to sweep, in ticks
     * @param nowMillis     the clock
     * @return the timer task
     */
    @NotNull
    public static Task start(@NotNull ResourcePackPromptTracker tracker, long intervalTicks,
                             @NotNull LongSupplier nowMillis) {
        requireNonNull(tracker, "tracker");
        requireNonNull(nowMillis, "nowMillis");
        long interval = Math.max(1L, intervalTicks);
        return Schedulers.runTimer(TaskContext.async(),
                () -> tracker.sweepTimeouts(nowMillis.getAsLong()), interval, interval);
    }
}
