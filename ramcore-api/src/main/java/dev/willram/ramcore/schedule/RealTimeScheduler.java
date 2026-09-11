package dev.willram.ramcore.schedule;

import dev.willram.ramcore.exception.RamPreconditions;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.Task;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.terminable.Terminable;
import dev.willram.ramcore.utils.RamLog;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Runs {@link Job}s on real-world time (cron / interval / daily). A once-per-second async timer
 * computes due jobs and executes each on its declared {@link TaskContext}. On registration a job's
 * {@link MissedRunPolicy} decides what to do about runs missed since its last recorded run.
 *
 * <p>State ({@link JobState}) is held in memory here; persist it in a {@code Store<String, JobState>}
 * and seed {@link #register(Job, JobState)} on startup to survive restarts. Uses an injectable
 * {@link Clock} for deterministic tests. Stability: experimental.</p>
 */
public final class RealTimeScheduler implements Terminable {
    private static final int MAX_SCAN = 100_000;

    private final Clock clock;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final Map<String, JobState> states = new ConcurrentHashMap<>();
    private volatile Task ticker;
    private volatile boolean closed;

    public RealTimeScheduler() {
        this(Clock.systemUTC());
    }

    public RealTimeScheduler(@NotNull Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    /** Registers a fresh job (no prior state); its next run is computed from now. */
    public void register(@NotNull Job job) {
        register(job, JobState.fresh());
    }

    /**
     * Registers a job with a previously persisted state, applying its missed-run policy for the gap
     * between {@code previous.lastRun} and now.
     *
     * @param job      the job
     * @param previous the persisted state (use {@link JobState#fresh()} for a new job)
     */
    public void register(@NotNull Job job, @NotNull JobState previous) {
        requireNonNull(job, "job");
        requireNonNull(previous, "previous");
        RamPreconditions.checkArgument(!this.jobs.containsKey(job.id()),
                "job '" + job.id() + "' already registered", "use a unique job id or unregister first");
        this.jobs.put(job.id(), job);

        Instant now = this.clock.instant();
        if (previous.lastRun() != null) {
            applyMissedRuns(job, Instant.ofEpochMilli(previous.lastRun()), now);
        }
        Instant next = job.schedule().nextAfter(now, job.zone()).orElse(null);
        Long lastRun = previous.lastRun();
        this.states.put(job.id(), new JobState(lastRun, next == null ? null : next.toEpochMilli()));
    }

    public void unregister(@NotNull String jobId) {
        this.jobs.remove(requireNonNull(jobId, "jobId"));
        this.states.remove(jobId);
    }

    @NotNull
    public Optional<JobState> state(@NotNull String jobId) {
        return Optional.ofNullable(this.states.get(jobId));
    }

    /** Starts the once-per-second async ticker. */
    public void start() {
        RamPreconditions.checkState(!this.closed, "scheduler is closed", "create a new scheduler");
        if (this.ticker != null) {
            return;
        }
        this.ticker = Schedulers.runTimer(TaskContext.async(), () -> tick(this.clock.instant()), 20L, 20L);
    }

    /** Stops the ticker. */
    public void stop() {
        if (this.ticker != null) {
            this.ticker.stop();
            this.ticker = null;
        }
    }

    /**
     * Runs any jobs due at {@code now}. Public for deterministic tests; the ticker calls it each
     * second.
     *
     * @param now the current instant
     */
    public void tick(@NotNull Instant now) {
        requireNonNull(now, "now");
        for (Job job : this.jobs.values()) {
            runDue(job, now);
        }
    }

    private void runDue(@NotNull Job job, @NotNull Instant now) {
        JobState state = this.states.get(job.id());
        if (state == null || state.nextRun() == null) {
            return;
        }
        Instant next = Instant.ofEpochMilli(state.nextRun());
        Long lastRun = state.lastRun();
        int cap = job.policy().catchUp() ? job.policy().max() : 1;
        int ran = 0;
        while (next != null && !now.isBefore(next) && ran < cap) {
            execute(job);
            lastRun = next.toEpochMilli();
            ran++;
            next = job.schedule().nextAfter(next, job.zone()).orElse(null);
        }
        // if still behind after the cap, jump past now to avoid a backlog storm
        if (next != null && !now.isBefore(next)) {
            next = job.schedule().nextAfter(now, job.zone()).orElse(null);
        }
        this.states.put(job.id(), new JobState(lastRun, next == null ? null : next.toEpochMilli()));
    }

    private void applyMissedRuns(@NotNull Job job, @NotNull Instant lastRun, @NotNull Instant now) {
        if (!job.policy().catchUp()) {
            return; // SKIP: drop all missed runs
        }
        int missed = 0;
        Instant cursor = lastRun;
        for (int i = 0; i < MAX_SCAN; i++) {
            Optional<Instant> next = job.schedule().nextAfter(cursor, job.zone());
            if (next.isEmpty() || next.get().isAfter(now)) {
                break;
            }
            cursor = next.get();
            missed++;
        }
        int toRun = Math.min(missed, job.policy().max());
        for (int i = 0; i < toRun; i++) {
            execute(job);
        }
    }

    private void execute(@NotNull Job job) {
        Schedulers.run(job.context(), () -> {
            try {
                job.task().run();
            } catch (RuntimeException failure) {
                RamLog.warn("scheduled job '" + job.id() + "' threw", failure);
            }
        });
    }

    @Override
    public void close() {
        this.closed = true;
        stop();
        this.jobs.clear();
        this.states.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
