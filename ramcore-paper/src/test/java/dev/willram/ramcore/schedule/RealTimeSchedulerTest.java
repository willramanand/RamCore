package dev.willram.ramcore.schedule;

import dev.willram.ramcore.scheduler.SchedulerBackends;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.testkit.FakeClock;
import dev.willram.ramcore.testkit.FakeScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RealTimeSchedulerTest {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private FakeScheduler scheduler;
    private FakeClock clock;

    @BeforeEach
    void setUp() {
        this.scheduler = FakeScheduler.install();
        this.clock = FakeClock.at(T0);
    }

    @AfterEach
    void tearDown() {
        this.scheduler.close();
        assertTrue(SchedulerBackends.isDefault());
    }

    private Job job(AtomicInteger runs, MissedRunPolicy policy) {
        return new Job("j", Schedule.every(Duration.ofMinutes(1)), UTC, policy, TaskContext.global(),
                runs::incrementAndGet);
    }

    @Test
    public void runsDueJob() {
        AtomicInteger runs = new AtomicInteger();
        RealTimeScheduler sched = new RealTimeScheduler(this.clock);
        sched.register(job(runs, MissedRunPolicy.skip()));

        sched.tick(this.clock.instant()); // not due yet
        assertEquals(0, runs.get());

        this.clock.advance(Duration.ofSeconds(61));
        sched.tick(this.clock.instant());
        this.scheduler.runAll();
        assertEquals(1, runs.get());
    }

    @Test
    public void skipsMissedRunsOnRegister() {
        AtomicInteger runs = new AtomicInteger();
        RealTimeScheduler sched = new RealTimeScheduler(this.clock);
        JobState prior = new JobState(T0.minusSeconds(600).toEpochMilli(), null); // last ran 10 min ago
        sched.register(job(runs, MissedRunPolicy.skip()), prior);
        this.scheduler.runAll();
        assertEquals(0, runs.get());
    }

    @Test
    public void catchesUpMissedRunsUpToMax() {
        AtomicInteger runs = new AtomicInteger();
        RealTimeScheduler sched = new RealTimeScheduler(this.clock);
        JobState prior = new JobState(T0.minusSeconds(600).toEpochMilli(), null); // ~10 missed at 1/min
        sched.register(job(runs, MissedRunPolicy.catchUp(3)), prior);
        this.scheduler.runAll();
        assertEquals(3, runs.get());
    }

    @Test
    public void duplicateJobRejectedAndUnregisterWorks() {
        RealTimeScheduler sched = new RealTimeScheduler(this.clock);
        sched.register(Job.of("j", Schedule.every(Duration.ofMinutes(1)), () -> {
        }));
        assertTrue(sched.state("j").isPresent());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sched.register(Job.of("j", Schedule.every(Duration.ofMinutes(1)), () -> {
                })));
        sched.unregister("j");
        assertTrue(sched.state("j").isEmpty());
    }
}
