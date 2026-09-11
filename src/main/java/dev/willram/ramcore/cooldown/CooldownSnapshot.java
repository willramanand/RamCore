package dev.willram.ramcore.cooldown;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * The persisted shape of one cooldown entry.
 *
 * @param lastTestedMillis when the cooldown was last consumed, or -1 when never
 * @param timeoutMillis    the cooldown length
 */
public record CooldownSnapshot(long lastTestedMillis, long timeoutMillis) {

    public static final long NEVER = -1L;

    /**
     * Captures a cooldown.
     *
     * @param cooldown the cooldown
     * @return the snapshot
     */
    @NotNull
    public static CooldownSnapshot of(@NotNull Cooldown cooldown) {
        Objects.requireNonNull(cooldown, "cooldown");
        OptionalLong lastTested = cooldown.getLastTested();
        return new CooldownSnapshot(lastTested.orElse(NEVER), cooldown.getTimeout());
    }

    /**
     * Rebuilds a cooldown.
     *
     * @return a cooldown with the recorded timeout and last-tested time
     */
    @NotNull
    public Cooldown toCooldown() {
        Cooldown cooldown = Cooldown.of(this.timeoutMillis, TimeUnit.MILLISECONDS);
        if (this.lastTestedMillis != NEVER) {
            cooldown.setLastTested(this.lastTestedMillis);
        }
        return cooldown;
    }

    /**
     * Whether the cooldown has fully elapsed at the given time (nothing worth persisting).
     *
     * @param nowMillis the current time
     * @return true when expired or never tested
     */
    public boolean expiredAt(long nowMillis) {
        return this.lastTestedMillis == NEVER || nowMillis - this.lastTestedMillis > this.timeoutMillis;
    }
}
