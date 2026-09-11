package dev.willram.ramcore.schedule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The persisted timing state of a job: when it last ran and when it should next run. Persist this in
 * a {@code Store<String, JobState>} to survive restarts and drive the missed-run policy.
 *
 * @param lastRun the last run instant (epoch millis), or {@code null} if never run
 * @param nextRun the next scheduled run instant (epoch millis), or {@code null} if none
 */
public record JobState(@Nullable Long lastRun, @Nullable Long nextRun) {

    /** A never-run state. */
    @NotNull
    public static JobState fresh() {
        return new JobState(null, null);
    }
}
