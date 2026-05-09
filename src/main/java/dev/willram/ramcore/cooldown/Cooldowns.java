package dev.willram.ramcore.cooldown;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Facade for common cooldown and throttle stores.
 */
public final class Cooldowns {

    @NotNull
    public static <K> CooldownTracker<K> tracker(@NotNull Cooldown cooldown) {
        return CooldownTracker.create(cooldown);
    }

    @NotNull
    public static CooldownTracker<CooldownKey> grouped(@NotNull Cooldown cooldown) {
        return CooldownTracker.create(cooldown);
    }

    @NotNull
    public static ActionThrottle<Object> throttle(@NotNull Cooldown cooldown) {
        return ActionThrottle.create(cooldown);
    }

    @NotNull
    public static ComposedCooldownMap<Player, UUID> players(@NotNull Cooldown cooldown) {
        return ComposedCooldownMap.create(cooldown, Player::getUniqueId);
    }

    @NotNull
    public static ComposedCooldownMap<OfflinePlayer, UUID> offlinePlayers(@NotNull Cooldown cooldown) {
        return ComposedCooldownMap.create(cooldown, OfflinePlayer::getUniqueId);
    }

    @NotNull
    public static CooldownKey key(@NotNull String group, @NotNull Object key) {
        return CooldownKey.of(group, key);
    }

    private Cooldowns() {
    }
}
