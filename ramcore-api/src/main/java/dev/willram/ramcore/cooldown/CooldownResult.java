package dev.willram.ramcore.cooldown;

import org.jetbrains.annotations.NotNull;

/**
 * Result of testing a cooldown key.
 */
public record CooldownResult<K>(
        @NotNull K key,
        boolean allowed,
        long remainingMillis,
        long timeoutMillis,
        long testedAtMillis,
        long expiresAtMillis
) {

    @NotNull
    public static <K> CooldownResult<K> allowed(@NotNull K key, long timeoutMillis, long testedAtMillis) {
        return new CooldownResult<>(key, true, 0L, timeoutMillis, testedAtMillis, testedAtMillis + timeoutMillis);
    }

    @NotNull
    public static <K> CooldownResult<K> denied(@NotNull K key, long remainingMillis, long timeoutMillis, long testedAtMillis, long expiresAtMillis) {
        return new CooldownResult<>(key, false, Math.max(0L, remainingMillis), timeoutMillis, testedAtMillis, expiresAtMillis);
    }

    public boolean denied() {
        return !this.allowed;
    }

    public long remainingSecondsCeil() {
        return (long) Math.ceil(this.remainingMillis / 1000.0d);
    }
}
