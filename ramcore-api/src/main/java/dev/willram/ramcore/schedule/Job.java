package dev.willram.ramcore.schedule;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.scheduler.TaskContext;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;

import static java.util.Objects.requireNonNull;

/**
 * A scheduled real-time job: an id, its {@link Schedule}, the zone the schedule is evaluated in, its
 * {@link MissedRunPolicy}, the {@link TaskContext} the task runs on, and the task.
 *
 * @param id       the unique job id
 * @param schedule when it runs
 * @param zone     the time zone for the schedule
 * @param policy   how missed runs are handled
 * @param context  the scheduler context the task executes on
 * @param task     the work
 */
public record Job(@NotNull String id, @NotNull Schedule schedule, @NotNull ZoneId zone,
                  @NotNull MissedRunPolicy policy, @NotNull TaskContext context, @NotNull Runnable task) {

    public Job {
        RamPreconditions.notBlank(id, "id");
        requireNonNull(schedule, "schedule");
        requireNonNull(zone, "zone");
        requireNonNull(policy, "policy");
        requireNonNull(context, "context");
        requireNonNull(task, "task");
    }

    /** A job in the system default zone that skips missed runs and runs on the global scheduler. */
    @NotNull
    public static Job of(@NotNull String id, @NotNull Schedule schedule, @NotNull Runnable task) {
        return new Job(id, schedule, ZoneId.systemDefault(), MissedRunPolicy.skip(), TaskContext.global(), task);
    }
}
