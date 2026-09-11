package dev.willram.ramcore.schedule;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

/**
 * What a job does about runs it missed while the server was down (or the scheduler was stopped).
 *
 * @param catchUp whether missed runs are replayed
 * @param max     the maximum number of missed runs to replay (only when {@code catchUp})
 */
public record MissedRunPolicy(boolean catchUp, int max) {

    public MissedRunPolicy {
        if (catchUp) {
            RamPreconditions.checkArgument(max > 0, "catch-up max must be positive", "pass max > 0");
        }
    }

    /** Skip missed runs; only future runs fire. */
    @NotNull
    public static MissedRunPolicy skip() {
        return new MissedRunPolicy(false, 0);
    }

    /** Replay up to {@code max} missed runs immediately, then resume the schedule. */
    @NotNull
    public static MissedRunPolicy catchUp(int max) {
        return new MissedRunPolicy(true, max);
    }
}
