package dev.willram.ramcore.ai;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * Lightweight runtime debug state for custom goals.
 */
public final class MobGoalDebugState {
    private MobGoalDebugStatus status = MobGoalDebugStatus.IDLE;
    private int activations;
    private int ticks;
    private Instant lastActivatedAt;
    private Instant lastStoppedAt;

    public synchronized void started(@NotNull Instant now) {
        this.status = MobGoalDebugStatus.RUNNING;
        this.activations++;
        this.lastActivatedAt = now;
    }

    public synchronized void stopped(@NotNull Instant now) {
        this.status = MobGoalDebugStatus.STOPPED;
        this.lastStoppedAt = now;
    }

    public synchronized void ticked() {
        this.ticks++;
    }

    @NotNull
    public synchronized MobGoalDebugStatus status() {
        return this.status;
    }

    public synchronized int activations() {
        return this.activations;
    }

    public synchronized int ticks() {
        return this.ticks;
    }

    @NotNull
    public synchronized Optional<Instant> lastActivatedAt() {
        return Optional.ofNullable(this.lastActivatedAt);
    }

    @NotNull
    public synchronized Optional<Instant> lastStoppedAt() {
        return Optional.ofNullable(this.lastStoppedAt);
    }
}
