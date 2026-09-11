package dev.willram.ramcore.cooldown;

import org.jetbrains.annotations.NotNull;

/**
 * Callback fired when a tracked cooldown is swept after expiry.
 */
@FunctionalInterface
public interface CooldownExpiryListener<K> {
    void expired(@NotNull K key, @NotNull Cooldown cooldown);
}
