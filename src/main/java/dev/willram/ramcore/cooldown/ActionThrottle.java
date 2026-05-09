package dev.willram.ramcore.cooldown;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Per-action cooldown throttle backed by grouped cooldown keys.
 */
public final class ActionThrottle<K> {
    private final CooldownTracker<CooldownKey> tracker;

    private ActionThrottle(@NotNull Cooldown base) {
        this.tracker = CooldownTracker.create(Objects.requireNonNull(base, "base"));
    }

    @NotNull
    public static <K> ActionThrottle<K> create(@NotNull Cooldown base) {
        return new ActionThrottle<>(base);
    }

    @NotNull
    public CooldownResult<CooldownKey> test(@NotNull K key, @NotNull String action) {
        return this.tracker.test(cooldownKey(action, key));
    }

    @NotNull
    public CooldownResult<CooldownKey> peek(@NotNull K key, @NotNull String action) {
        return this.tracker.peek(cooldownKey(action, key));
    }

    public boolean allowed(@NotNull K key, @NotNull String action) {
        return test(key, action).allowed();
    }

    public boolean active(@NotNull K key, @NotNull String action) {
        return peek(key, action).denied();
    }

    public void reset(@NotNull K key, @NotNull String action) {
        this.tracker.reset(cooldownKey(action, key));
    }

    public long remainingMillis(@NotNull K key, @NotNull String action) {
        return this.tracker.remainingMillis(cooldownKey(action, key));
    }

    public long remainingTime(@NotNull K key, @NotNull String action, @NotNull TimeUnit unit) {
        return this.tracker.remainingTime(cooldownKey(action, key), unit);
    }

    @NotNull
    public ActionThrottle<K> onExpire(@NotNull CooldownExpiryListener<CooldownKey> listener) {
        this.tracker.onExpire(listener);
        return this;
    }

    @NotNull
    public CooldownTracker<CooldownKey> tracker() {
        return this.tracker;
    }

    @NotNull
    public CooldownKey cooldownKey(@NotNull String action, @NotNull K key) {
        return CooldownKey.of(action, Objects.requireNonNull(key, "key"));
    }
}
