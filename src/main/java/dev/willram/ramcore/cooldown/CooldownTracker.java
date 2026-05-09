package dev.willram.ramcore.cooldown;

import dev.willram.ramcore.time.Time;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Higher-level cooldown store with result objects, expiry callbacks, and cleanup helpers.
 */
public final class CooldownTracker<K> {
    private final CooldownMap<K> cooldowns;
    private final List<CooldownExpiryListener<K>> expiryListeners = new ArrayList<>();

    private CooldownTracker(@NotNull Cooldown base) {
        this.cooldowns = CooldownMap.create(Objects.requireNonNull(base, "base"));
    }

    @NotNull
    public static <K> CooldownTracker<K> create(@NotNull Cooldown base) {
        return new CooldownTracker<>(base);
    }

    @NotNull
    public Cooldown base() {
        return this.cooldowns.getBase();
    }

    @NotNull
    public Cooldown cooldown(@NotNull K key) {
        return this.cooldowns.get(Objects.requireNonNull(key, "key"));
    }

    @NotNull
    public CooldownResult<K> test(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        Cooldown cooldown = cooldown(key);
        long now = Time.nowMillis();
        if (cooldown.testSilently()) {
            cooldown.setLastTested(now);
            return CooldownResult.allowed(key, cooldown.getTimeout(), now);
        }

        return deniedResult(key, cooldown, now);
    }

    @NotNull
    public CooldownResult<K> peek(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        Cooldown cooldown = cooldown(key);
        long now = Time.nowMillis();
        if (cooldown.testSilently()) {
            return CooldownResult.allowed(key, cooldown.getTimeout(), now);
        }

        return deniedResult(key, cooldown, now);
    }

    public boolean allowed(@NotNull K key) {
        return test(key).allowed();
    }

    public boolean active(@NotNull K key) {
        return peek(key).denied();
    }

    public void reset(@NotNull K key) {
        this.cooldowns.reset(Objects.requireNonNull(key, "key"));
    }

    public void reset(@NotNull K key, long timeMillis) {
        this.cooldowns.setLastTested(Objects.requireNonNull(key, "key"), timeMillis);
    }

    public long remainingMillis(@NotNull K key) {
        return this.cooldowns.remainingMillis(Objects.requireNonNull(key, "key"));
    }

    public long remainingTime(@NotNull K key, @NotNull TimeUnit unit) {
        return this.cooldowns.remainingTime(Objects.requireNonNull(key, "key"), Objects.requireNonNull(unit, "unit"));
    }

    @NotNull
    public OptionalLong lastTested(@NotNull K key) {
        return this.cooldowns.getLastTested(Objects.requireNonNull(key, "key"));
    }

    @NotNull
    public CooldownTracker<K> onExpire(@NotNull CooldownExpiryListener<K> listener) {
        this.expiryListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @NotNull
    public List<K> sweepExpired() {
        List<Map.Entry<K, Cooldown>> expired = this.cooldowns.getAll().entrySet().stream()
                .filter(entry -> expired(entry.getValue()))
                .toList();

        for (Map.Entry<K, Cooldown> entry : expired) {
            remove(entry.getKey());
            for (CooldownExpiryListener<K> listener : this.expiryListeners) {
                listener.expired(entry.getKey(), entry.getValue());
            }
        }

        return expired.stream()
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean remove(@NotNull K key) {
        return this.cooldowns.getAll().remove(Objects.requireNonNull(key, "key")) != null;
    }

    public void clear() {
        this.cooldowns.getAll().clear();
    }

    public int size() {
        return this.cooldowns.getAll().size();
    }

    @NotNull
    public Set<K> keys() {
        return Set.copyOf(this.cooldowns.getAll().keySet());
    }

    @NotNull
    public Collection<Cooldown> values() {
        return List.copyOf(this.cooldowns.getAll().values());
    }

    @NotNull
    public Map<K, Cooldown> asMap() {
        return this.cooldowns.getAll();
    }

    private CooldownResult<K> deniedResult(K key, Cooldown cooldown, long now) {
        long remainingMillis = cooldown.remainingMillis();
        long lastTested = cooldown.getLastTested().orElse(0L);
        long expiresAt = lastTested == 0L ? now : lastTested + cooldown.getTimeout();
        return CooldownResult.denied(key, remainingMillis, cooldown.getTimeout(), now, expiresAt);
    }

    private static boolean expired(Cooldown cooldown) {
        return cooldown.getLastTested().isPresent() && cooldown.testSilently();
    }
}
